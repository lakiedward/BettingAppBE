package com.valuebet.backend.service;

import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.OddsSnapshot;
import com.valuebet.backend.domain.model.Outcome;
import com.valuebet.backend.domain.repository.EventRepository;
import com.valuebet.backend.domain.repository.OddsSnapshotRepository;
import com.valuebet.backend.web.dto.EventOddsDto;
import com.valuebet.backend.web.dto.EventOutcomeOddsDto;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventQueryService {

    private final EventRepository eventRepository;
    private final OddsSnapshotRepository oddsSnapshotRepository;
    private final OddsNormalizationService oddsNormalizationService;
    private final ProbabilityService probabilityService;

    public EventOddsDto getEventOdds(UUID eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        List<OddsSnapshot> snapshots = oddsSnapshotRepository.findTop200ByEventIdOrderByCapturedAtDesc(eventId);

        Map<MarketType, Map<String, Map<Outcome, OddsSnapshot>>> latestPerMarket = new HashMap<>();
        Map<MarketType, Map<Outcome, OddsSnapshot>> closingPerMarket = new HashMap<>();

        for (OddsSnapshot snapshot : snapshots) {
            MarketType marketType = snapshot.getMarketType();
            String bookmaker = snapshot.getBookmaker();
            Outcome outcome = snapshot.getOutcome();
            if (marketType == null || bookmaker == null || outcome == null) {
                continue;
            }
            latestPerMarket
                .computeIfAbsent(marketType, key -> new HashMap<>())
                .computeIfAbsent(bookmaker, key -> new EnumMap<>(Outcome.class))
                .merge(outcome, snapshot, (existing, incoming) -> isAfter(incoming, existing) ? incoming : existing);
            if (snapshot.isClosingLine()) {
                closingPerMarket
                    .computeIfAbsent(marketType, key -> new EnumMap<>(Outcome.class))
                    .merge(outcome, snapshot, (existing, incoming) -> isAfter(incoming, existing) ? incoming : existing);
            }
        }

        List<EventOutcomeOddsDto> outcomeDtos = new ArrayList<>();
        latestPerMarket.forEach((marketType, byBookmaker) -> {
            List<Map<Outcome, Double>> probabilityInputs = new ArrayList<>();
            Map<Outcome, OddsSnapshot> bestOddsPerOutcome = new EnumMap<>(Outcome.class);
            Map<Outcome, String> bestBookmakerPerOutcome = new EnumMap<>(Outcome.class);

            byBookmaker.forEach((bookmaker, outcomeMap) -> {
                Map<Outcome, Double> implied = new EnumMap<>(Outcome.class);
                outcomeMap.forEach((outcome, snapshot) -> {
                    double probability = optionalBigDecimal(snapshot.getImpliedProbability());
                    if (probability == 0.0d && snapshot.getOdds() != null && snapshot.getOdds().doubleValue() > 0.0d) {
                        probability = 1.0d / snapshot.getOdds().doubleValue();
                    }
                    implied.put(outcome, probability);

                    OddsSnapshot currentBest = bestOddsPerOutcome.get(outcome);
                    if (currentBest == null || compareOdds(snapshot.getOdds(), currentBest.getOdds()) > 0) {
                        bestOddsPerOutcome.put(outcome, snapshot);
                        bestBookmakerPerOutcome.put(outcome, bookmaker);
                    }
                });
                if (!implied.isEmpty()) {
                    probabilityInputs.add(implied);
                }
            });

            Map<Outcome, Double> marketProbabilities = probabilityInputs.isEmpty()
                ? Map.of()
                : oddsNormalizationService.aggregateAcrossBookmakers(probabilityInputs);
            Map<Outcome, Double> trueProbabilities = probabilityService.estimateTrueProb(event, marketType, marketProbabilities);

            Set<Outcome> outcomes = collectOutcomes(bestOddsPerOutcome, marketProbabilities, trueProbabilities);
            Map<Outcome, OddsSnapshot> closingMap = closingPerMarket.getOrDefault(marketType, Map.of());

            outcomes.forEach(outcome -> {
                OddsSnapshot bestSnapshot = bestOddsPerOutcome.get(outcome);
                BigDecimal bestOdds = bestSnapshot != null ? bestSnapshot.getOdds() : null;
                OddsSnapshot closingSnapshot = closingMap.get(outcome);
                Double closingOdds = closingSnapshot != null && closingSnapshot.getOdds() != null
                    ? closingSnapshot.getOdds().doubleValue()
                    : null;

                outcomeDtos.add(new EventOutcomeOddsDto(
                    marketType,
                    outcome,
                    bestBookmakerPerOutcome.get(outcome),
                    bestOdds != null ? bestOdds.doubleValue() : 0.0d,
                    closingOdds,
                    marketProbabilities.getOrDefault(outcome, 0.0d),
                    trueProbabilities.getOrDefault(outcome, 0.0d)
                ));
            });
        });

        return new EventOddsDto(
            event.getId(),
            event.getCompetition(),
            event.getHomeTeam(),
            event.getAwayTeam(),
            event.getStartTime(),
            outcomeDtos
        );
    }

    private boolean isAfter(OddsSnapshot a, OddsSnapshot b) {
        Instant aCaptured = a.getCapturedAt();
        Instant bCaptured = b != null ? b.getCapturedAt() : null;
        return bCaptured == null || (aCaptured != null && aCaptured.isAfter(bCaptured));
    }

    private int compareOdds(BigDecimal first, BigDecimal second) {
        BigDecimal left = first == null ? BigDecimal.ZERO : first;
        BigDecimal right = second == null ? BigDecimal.ZERO : second;
        return left.compareTo(right);
    }

    private double optionalBigDecimal(BigDecimal value) {
        return value == null ? 0.0d : value.doubleValue();
    }

    private Set<Outcome> collectOutcomes(Map<Outcome, OddsSnapshot> bestOdds,
                                         Map<Outcome, Double> marketProbabilities,
                                         Map<Outcome, Double> trueProbabilities) {
        return List.of(bestOdds.keySet(),
            marketProbabilities.keySet(),
            trueProbabilities.keySet()).stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
    }
}
