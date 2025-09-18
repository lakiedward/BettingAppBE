package com.valuebet.backend.service;

import com.valuebet.backend.config.ValuebetProperties;
import com.valuebet.backend.domain.model.Bookmaker;
import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import com.valuebet.backend.domain.model.OddsSnapshot;
import com.valuebet.backend.domain.model.ValueOpportunity;
import com.valuebet.backend.domain.repository.BookmakerRepository;
import com.valuebet.backend.domain.repository.EventRepository;
import com.valuebet.backend.domain.repository.OddsSnapshotRepository;
import com.valuebet.backend.domain.repository.ValueOpportunityRepository;
import com.valuebet.backend.integration.odds.OddsProviderClient;
import com.valuebet.backend.integration.odds.ProviderOddsDto;
import com.valuebet.backend.websocket.ValueBetWsPublisher;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OddsIngestionService {

    private static final double MIN_EXPECTED_VALUE_THRESHOLD = 0.015d;
    private static final Duration CLOSING_WINDOW = Duration.ofMinutes(5);

    private final OddsProviderClient oddsProviderClient;
    private final EventRepository eventRepository;
    private final OddsSnapshotRepository oddsSnapshotRepository;
    private final ValueOpportunityRepository valueOpportunityRepository;
    private final BookmakerRepository bookmakerRepository;
    private final OddsNormalizationService oddsNormalizationService;
    private final ProbabilityService probabilityService;
    private final ValueDetectionService valueDetectionService;
    private final ValuebetProperties valuebetProperties;
    private final ValueBetWsPublisher valueBetWsPublisher;

    @Transactional
    public Map<EventMarketKey, Map<Outcome, Double>> ingestUpcomingOdds(Duration horizon) {
        Duration effectiveHorizon = horizon != null ? horizon : valuebetProperties.ingestionHorizon();
        List<ProviderOddsDto> providerOdds = oddsProviderClient.fetchUpcomingOdds(effectiveHorizon);
        if (providerOdds == null || providerOdds.isEmpty()) {
            log.debug("No provider odds fetched for horizon {}", effectiveHorizon);
            return Collections.emptyMap();
        }

        Map<String, Event> eventCache = new HashMap<>();
        Map<UUID, Event> eventById = new HashMap<>();
        Map<String, Bookmaker> bookmakerCache = new HashMap<>();
        List<OddsSnapshot> snapshots = new ArrayList<>();
        Map<EventMarketBookmakerKey, EnumMap<Outcome, Double>> bookmakerProbabilities = new HashMap<>();
        Map<EventMarketKey, EnumMap<Outcome, BestOdds>> bestOddsPerMarket = new HashMap<>();
        Map<EventMarketKey, Map<String, EnumMap<Outcome, OddsSnapshot>>> closingCandidates = new HashMap<>();
        Instant captureTime = Instant.now();

        for (ProviderOddsDto dto : providerOdds) {
            Event event = resolveEvent(dto, eventCache);
            if (event == null) {
                continue;
            }
            eventById.put(event.getId(), event);

            Bookmaker bookmaker = resolveBookmaker(dto.bookmakerKey(), bookmakerCache);

            double impliedProbability = computeImpliedProbability(dto.decimalOdds());
            EventMarketKey marketKey = new EventMarketKey(event.getId(), dto.marketType());

            EnumMap<Outcome, Double> outcomeProbabilities = bookmakerProbabilities.computeIfAbsent(
                new EventMarketBookmakerKey(marketKey, dto.bookmakerKey()),
                key -> new EnumMap<>(Outcome.class)
            );
            outcomeProbabilities.put(dto.outcome(), impliedProbability);

            BigDecimal scaledOdds = scaleDecimal(dto.decimalOdds());
            if (bookmaker != null && scaledOdds != null) {
                EnumMap<Outcome, BestOdds> bestByOutcome = bestOddsPerMarket.computeIfAbsent(
                    marketKey,
                    key -> new EnumMap<>(Outcome.class)
                );
                BestOdds currentBest = bestByOutcome.get(dto.outcome());
                if (currentBest == null || scaledOdds.compareTo(currentBest.odds()) > 0) {
                    bestByOutcome.put(dto.outcome(), new BestOdds(scaledOdds, bookmaker));
                }
            }

            OddsSnapshot snapshot = OddsSnapshot.builder()
                .event(event)
                .marketType(dto.marketType())
                .outcome(dto.outcome())
                .line(null)
                .bookmaker(dto.bookmakerKey())
                .odds(scaledOdds)
                .impliedProbability(scaleProbability(impliedProbability))
                .capturedAt(captureTime)
                .closingLine(false)
                .build();
            snapshots.add(snapshot);

            if (isWithinClosingWindow(event, captureTime) && dto.bookmakerKey() != null) {
                Map<String, EnumMap<Outcome, OddsSnapshot>> byBookmaker = closingCandidates.computeIfAbsent(
                    marketKey,
                    key -> new HashMap<>()
                );
                EnumMap<Outcome, OddsSnapshot> byOutcome = byBookmaker.computeIfAbsent(
                    dto.bookmakerKey(),
                    key -> new EnumMap<>(Outcome.class)
                );
                byOutcome.put(dto.outcome(), snapshot);
            }
        }

        applyClosingLine(closingCandidates, bestOddsPerMarket);

        if (!snapshots.isEmpty()) {
            oddsSnapshotRepository.saveAll(snapshots);
        }

        Map<EventMarketKey, List<Map<Outcome, Double>>> normalizedPerMarket = new HashMap<>();
        bookmakerProbabilities.forEach((key, impliedMap) -> {
            Map<Outcome, Double> noVig = oddsNormalizationService.removeVig(impliedMap);
            normalizedPerMarket.computeIfAbsent(key.market(), k -> new ArrayList<>()).add(noVig);
        });

        Map<EventMarketKey, Map<Outcome, Double>> aggregated = new HashMap<>();
        normalizedPerMarket.forEach((marketKey, probMaps) -> {
            Map<Outcome, Double> medianProbabilities = oddsNormalizationService.aggregateAcrossBookmakers(probMaps);
            aggregated.put(marketKey, medianProbabilities);
        });

        List<ValueOpportunity> opportunities = new ArrayList<>();
        aggregated.forEach((marketKey, noVigProbabilities) -> {
            Event event = eventById.get(marketKey.eventId());
            if (event == null || noVigProbabilities == null || noVigProbabilities.isEmpty()) {
                return;
            }
            EnumMap<Outcome, BestOdds> bestOdds = bestOddsPerMarket.get(marketKey);
            if (bestOdds == null || bestOdds.isEmpty()) {
                return;
            }

            Map<Outcome, Double> trueProbabilities = probabilityService.estimateTrueProb(
                event,
                marketKey.marketType(),
                noVigProbabilities
            );

            trueProbabilities.forEach((outcome, pTrue) -> {
                BestOdds best = bestOdds.get(outcome);
                if (best == null || best.odds() == null || best.bookmaker() == null) {
                    return;
                }
                double oddsValue = best.odds().doubleValue();
                if (oddsValue <= 1.0d || pTrue == null || pTrue <= 0.0d) {
                    return;
                }
                double expectedValue = oddsValue * pTrue - 1.0d;
                if (expectedValue < MIN_EXPECTED_VALUE_THRESHOLD) {
                    return;
                }
                ValueOpportunity opportunity = valueDetectionService.detect(
                    event,
                    marketKey.marketType(),
                    outcome,
                    oddsValue,
                    best.bookmaker().getId(),
                    pTrue
                );
                opportunities.add(opportunity);
            });
        });

        if (!opportunities.isEmpty()) {
            valueOpportunityRepository.saveAll(opportunities);
            valueBetWsPublisher.publish(opportunities);
        }

        return aggregated;
    }

    private Event resolveEvent(ProviderOddsDto dto, Map<String, Event> cache) {
        String externalId = buildExternalId(dto);
        if (externalId == null) {
            log.warn("Skipping odds entry due to missing event identity: {}", dto);
            return null;
        }
        return cache.computeIfAbsent(externalId, key -> upsertEvent(dto, key));
    }

    private Bookmaker resolveBookmaker(String bookmakerKey, Map<String, Bookmaker> cache) {
        if (bookmakerKey == null || bookmakerKey.isBlank()) {
            return null;
        }
        return cache.computeIfAbsent(bookmakerKey, key ->
            bookmakerRepository.findByExternalKey(key)
                .orElseGet(() -> bookmakerRepository.save(Bookmaker.builder().externalKey(key).build()))
        );
    }

    private Event upsertEvent(ProviderOddsDto dto, String externalId) {
        List<String> teams = dto.teams() == null ? List.of() : dto.teams();
        String homeTeam = teams.size() > 0 ? teams.get(0) : null;
        String awayTeam = teams.size() > 1 ? teams.get(1) : null;
        String eventName = buildEventName(teams);
        return eventRepository.findByExternalId(externalId)
            .map(existing -> updateEvent(existing, dto, eventName, homeTeam, awayTeam))
            .orElseGet(() -> createEvent(dto, externalId, eventName, homeTeam, awayTeam));
    }

    private Event updateEvent(Event event, ProviderOddsDto dto, String eventName, String homeTeam, String awayTeam) {
        boolean dirty = false;
        if (!Objects.equals(event.getName(), eventName)) {
            event.setName(eventName);
            dirty = true;
        }
        if (!Objects.equals(event.getCompetition(), dto.league())) {
            event.setCompetition(dto.league());
            dirty = true;
        }
        if (!Objects.equals(event.getStartTime(), dto.startTime())) {
            event.setStartTime(dto.startTime());
            dirty = true;
        }
        if (!Objects.equals(event.getHomeTeam(), homeTeam)) {
            event.setHomeTeam(homeTeam);
            dirty = true;
        }
        if (!Objects.equals(event.getAwayTeam(), awayTeam)) {
            event.setAwayTeam(awayTeam);
            dirty = true;
        }
        return dirty ? eventRepository.save(event) : event;
    }

    private Event createEvent(ProviderOddsDto dto,
                              String externalId,
                              String eventName,
                              String homeTeam,
                              String awayTeam) {
        Event event = Event.builder()
            .externalId(externalId)
            .name(eventName)
            .competition(dto.league())
            .homeTeam(homeTeam)
            .awayTeam(awayTeam)
            .startTime(dto.startTime())
            .build();
        return eventRepository.save(event);
    }

    private String buildExternalId(ProviderOddsDto dto) {
        if (dto == null || dto.startTime() == null) {
            return null;
        }
        String league = Optional.ofNullable(dto.league()).orElse("unknown_league").toLowerCase(Locale.ROOT);
        String teamsPart = Optional.ofNullable(dto.teams())
            .filter(list -> !list.isEmpty())
            .map(list -> String.join("_", list).toLowerCase(Locale.ROOT))
            .orElse("unknown_teams");
        return league + "|" + teamsPart + "|" + dto.startTime().toString();
    }

    private String buildEventName(List<String> teams) {
        if (teams == null || teams.isEmpty()) {
            return "Unknown matchup";
        }
        if (teams.size() >= 2) {
            return teams.get(0) + " vs " + teams.get(1);
        }
        return teams.get(0);
    }

    private double computeImpliedProbability(BigDecimal decimalOdds) {
        if (decimalOdds == null) {
            return 0.0d;
        }
        double oddsValue = decimalOdds.doubleValue();
        if (oddsValue <= 0.0d) {
            return 0.0d;
        }
        return 1.0d / oddsValue;
    }

    private BigDecimal scaleDecimal(BigDecimal decimalOdds) {
        return Optional.ofNullable(decimalOdds)
            .map(value -> value.setScale(3, RoundingMode.HALF_UP))
            .orElse(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
    }

    private BigDecimal scaleProbability(double probability) {
        return BigDecimal.valueOf(probability).setScale(4, RoundingMode.HALF_UP);
    }

    private boolean isWithinClosingWindow(Event event, Instant referenceTime) {
        OffsetDateTime startTime = event.getStartTime();
        if (startTime == null) {
            return false;
        }
        Instant startInstant = startTime.toInstant();
        if (startInstant.isBefore(referenceTime)) {
            return false;
        }
        Duration delta = Duration.between(referenceTime, startInstant);
        return !delta.isNegative() && delta.compareTo(CLOSING_WINDOW) <= 0;
    }

    private void applyClosingLine(
        Map<EventMarketKey, Map<String, EnumMap<Outcome, OddsSnapshot>>> closingCandidates,
        Map<EventMarketKey, EnumMap<Outcome, BestOdds>> bestOddsPerMarket
    ) {
        List<String> priorityBookmakers = valuebetProperties.closingLine().priorityBookmakers();
        closingCandidates.forEach((marketKey, byBookmaker) -> {
            Set<Outcome> outcomes = collectOutcomes(byBookmaker);
            outcomes.forEach(outcome -> {
                OddsSnapshot snapshot = selectPriorityCandidate(byBookmaker, priorityBookmakers, outcome);
                if (snapshot == null) {
                    snapshot = selectBestOddsCandidate(byBookmaker, bestOddsPerMarket.get(marketKey), outcome);
                }
                if (snapshot == null) {
                    snapshot = findHighestOddsCandidate(byBookmaker, outcome);
                }
                if (snapshot != null) {
                    snapshot.setClosingLine(true);
                }
            });
        });
    }

    private Set<Outcome> collectOutcomes(Map<String, EnumMap<Outcome, OddsSnapshot>> byBookmaker) {
        EnumSet<Outcome> outcomes = EnumSet.noneOf(Outcome.class);
        byBookmaker.values().forEach(map -> outcomes.addAll(map.keySet()));
        return outcomes;
    }

    private OddsSnapshot selectPriorityCandidate(Map<String, EnumMap<Outcome, OddsSnapshot>> byBookmaker,
                                                 List<String> priorityBookmakers,
                                                 Outcome outcome) {
        if (priorityBookmakers == null) {
            return null;
        }
        for (String priority : priorityBookmakers) {
            OddsSnapshot candidate = getSnapshot(byBookmaker, priority, outcome);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private OddsSnapshot selectBestOddsCandidate(Map<String, EnumMap<Outcome, OddsSnapshot>> byBookmaker,
                                                 EnumMap<Outcome, BestOdds> bestOddsMap,
                                                 Outcome outcome) {
        if (bestOddsMap == null) {
            return null;
        }
        BestOdds best = bestOddsMap.get(outcome);
        if (best == null || best.bookmaker() == null) {
            return null;
        }
        return getSnapshot(byBookmaker, best.bookmaker().getExternalKey(), outcome);
    }

    private OddsSnapshot findHighestOddsCandidate(Map<String, EnumMap<Outcome, OddsSnapshot>> byBookmaker,
                                                  Outcome outcome) {
        return byBookmaker.values().stream()
            .map(map -> map.get(outcome))
            .filter(Objects::nonNull)
            .max(Comparator.comparing(snapshot -> snapshot.getOdds() == null ? BigDecimal.ZERO : snapshot.getOdds()))
            .orElse(null);
    }

    private OddsSnapshot getSnapshot(Map<String, EnumMap<Outcome, OddsSnapshot>> byBookmaker,
                                     String bookmakerKey,
                                     Outcome outcome) {
        if (bookmakerKey == null) {
            return null;
        }
        EnumMap<Outcome, OddsSnapshot> byOutcome = byBookmaker.get(bookmakerKey);
        if (byOutcome == null) {
            return null;
        }
        return byOutcome.get(outcome);
    }

    public record EventMarketKey(UUID eventId, MarketType marketType) {
    }

    private record EventMarketBookmakerKey(EventMarketKey market, String bookmakerKey) {
    }

    private record BestOdds(BigDecimal odds, Bookmaker bookmaker) {
    }
}
