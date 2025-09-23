package com.valuebet.backend.integration.odds;

import com.valuebet.backend.config.ValuebetProperties;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import com.valuebet.backend.integration.odds.config.TheOddsApiProperties;
import com.valuebet.backend.integration.odds.dto.TheOddsApiBookmakerDto;
import com.valuebet.backend.integration.odds.dto.TheOddsApiEventDto;
import com.valuebet.backend.integration.odds.dto.TheOddsApiMarketDto;
import com.valuebet.backend.integration.odds.dto.TheOddsApiOutcomeDto;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("!mock")
public class TheOddsApiClient implements OddsProviderClient {

    private static final Logger log = LoggerFactory.getLogger(TheOddsApiClient.class);
    private static final ParameterizedTypeReference<List<TheOddsApiEventDto>> EVENT_RESPONSE_TYPE =
        new ParameterizedTypeReference<>() { };

    private final RestTemplate restTemplate;
    private final TheOddsApiProperties properties;
    private final Retry retry;
    private final TimeLimiter timeLimiter;
    private final ValuebetProperties valuebetProperties;

    public TheOddsApiClient(RestTemplateBuilder restTemplateBuilder,
                            TheOddsApiProperties properties,
                            ValuebetProperties valuebetProperties,
                            RetryRegistry retryRegistry,
                            TimeLimiterRegistry timeLimiterRegistry) {
        this.properties = properties;
        this.valuebetProperties = valuebetProperties;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(properties.connectTimeout())
            .setReadTimeout(properties.readTimeout())
            .build();
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(properties.retry().maxAttempts())
            .waitDuration(properties.retry().waitDuration())
            .build();
        this.retry = retryRegistry.retry("theOddsApi", () -> retryConfig);

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(properties.timeLimiter().timeoutDuration())
            .build();
        this.timeLimiter = timeLimiterRegistry.timeLimiter("theOddsApi", () -> timeLimiterConfig);
    }

    @Override
    public List<ProviderOddsDto> fetchUpcomingOdds(Duration horizon) {
        return retry.executeSupplier(() -> {
            try {
                return timeLimiter.executeFutureSupplier(() ->
                    CompletableFuture.supplyAsync(() -> doFetchUpcomingOdds(horizon))
                );
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to fetch odds within time limit", ex);
            }
        });
    }

    private List<ProviderOddsDto> doFetchUpcomingOdds(Duration horizon) {
        List<String> leagueKeys = resolveTrackedLeagueKeys();
        LinkedHashSet<ProviderOddsDto> aggregatedOdds = new LinkedHashSet<>();

        for (String leagueKey : leagueKeys) {
            URI uri = buildOddsUri(horizon, leagueKey);
            log.debug("Fetching odds feed for league {}", leagueKey);
            log.info("Making API request to: {}", uri);
            ResponseEntity<List<TheOddsApiEventDto>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                null,
                EVENT_RESPONSE_TYPE
            );
            List<TheOddsApiEventDto> events = Optional.ofNullable(response.getBody()).orElse(Collections.emptyList());
            log.info("Received {} events from API for league {}", events.size(), leagueKey);
            List<ProviderOddsDto> providerOdds = events.stream()
                .filter(Objects::nonNull)
                .flatMap(event -> toProviderOdds(event).stream())
                .collect(Collectors.toList());
            log.info("Mapped to {} provider odds for league {}", providerOdds.size(), leagueKey);
            aggregatedOdds.addAll(providerOdds);
        }

        log.info("Aggregated {} provider odds across {} leagues", aggregatedOdds.size(), leagueKeys.size());
        return List.copyOf(aggregatedOdds);
    }

    private List<String> resolveTrackedLeagueKeys() {
        List<String> trackedLeagues = Optional.ofNullable(valuebetProperties)
            .map(ValuebetProperties::filter)
            .map(ValuebetProperties.FilterProperties::trackedLeagues)
            .orElse(List.of());

        LinkedHashSet<String> normalized = trackedLeagues.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalized.isEmpty()) {
            return List.of(properties.defaultSport());
        }

        return List.copyOf(normalized);
    }

    private List<ProviderOddsDto> toProviderOdds(TheOddsApiEventDto event) {
        if (event.bookmakers() == null || event.bookmakers().isEmpty()) {
            return List.of();
        }
        List<String> teams = extractTeams(event);
        OffsetDateTime startTime = event.commenceTime();
        return event.bookmakers().stream()
            .flatMap(bookmaker -> mapBookmaker(event, bookmaker, teams, startTime))
            .collect(Collectors.toList());
    }

    private Stream<ProviderOddsDto> mapBookmaker(TheOddsApiEventDto event,
                                                 TheOddsApiBookmakerDto bookmaker,
                                                 List<String> teams,
                                                 OffsetDateTime startTime) {
        if (bookmaker == null || bookmaker.markets() == null || bookmaker.markets().isEmpty()) {
            return Stream.empty();
        }
        String bookmakerKey = normalizeKey(bookmaker.key());
        if (bookmakerKey == null) {
            return Stream.empty();
        }
        return bookmaker.markets().stream()
            .flatMap(market -> mapMarket(event, market, bookmakerKey, teams, startTime));
    }

    private Stream<ProviderOddsDto> mapMarket(TheOddsApiEventDto event,
                                              TheOddsApiMarketDto market,
                                              String bookmakerKey,
                                              List<String> teams,
                                              OffsetDateTime startTime) {
        if (market == null || market.outcomes() == null || market.outcomes().isEmpty()) {
            return Stream.empty();
        }
        Optional<MarketType> marketType = mapMarketType(market.key());
        if (marketType.isEmpty()) {
            return Stream.empty();
        }
        MarketType resolvedMarket = marketType.get();
        return market.outcomes().stream()
            .map(outcome -> buildProviderOdds(event, resolvedMarket, bookmakerKey, teams, startTime, outcome))
            .flatMap(Optional::stream);
    }

    private Optional<ProviderOddsDto> buildProviderOdds(TheOddsApiEventDto event,
                                                        MarketType marketType,
                                                        String bookmakerKey,
                                                        List<String> teams,
                                                        OffsetDateTime startTime,
                                                        TheOddsApiOutcomeDto outcomeDto) {
        if (outcomeDto == null) {
            return Optional.empty();
        }
        Outcome outcome = mapOutcome(outcomeDto, event);
        if (outcome == null) {
            return Optional.empty();
        }
        BigDecimal price = outcomeDto.price();
        if (price == null) {
            return Optional.empty();
        }
        return Optional.of(new ProviderOddsDto(
            event.sportTitle(),
            teams,
            startTime,
            marketType,
            outcome,
            bookmakerKey,
            price
        ));
    }

    private List<String> extractTeams(TheOddsApiEventDto event) {
        List<String> teams = event.teams();
        if (teams != null && !teams.isEmpty()) {
            List<String> normalized = teams.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
            if (!normalized.isEmpty()) {
                return List.copyOf(normalized);
            }
        }
        List<String> inferred = new ArrayList<>();
        if (event.homeTeam() != null && !event.homeTeam().isBlank()) {
            inferred.add(event.homeTeam());
        }
        determineAwayTeam(event).ifPresent(inferred::add);
        return inferred.isEmpty() ? List.of() : List.copyOf(inferred);
    }

    private Optional<MarketType> mapMarketType(String marketKey) {
        if (marketKey == null) {
            return Optional.empty();
        }
        if ("h2h".equalsIgnoreCase(marketKey)) {
            return Optional.of(MarketType.ONE_X_TWO);
        }
        return Optional.empty();
    }

    private Outcome mapOutcome(TheOddsApiOutcomeDto outcomeDto, TheOddsApiEventDto event) {
        String name = normalizeName(outcomeDto.name());
        if (name == null) {
            return null;
        }
        if ("draw".equals(name) || "tie".equals(name)) {
            return Outcome.DRAW;
        }
        if ("home".equals(name)) {
            return Outcome.ONE;
        }
        if ("away".equals(name)) {
            return Outcome.TWO;
        }
        String home = normalizeName(event.homeTeam());
        if (home != null && home.equals(name)) {
            return Outcome.ONE;
        }
        Optional<String> away = determineAwayTeam(event).map(this::normalizeName);
        if (away.isPresent() && away.get().equals(name)) {
            return Outcome.TWO;
        }
        List<String> teams = event.teams();
        if (teams != null) {
            for (String team : teams) {
                String normalized = normalizeName(team);
                if (normalized == null || !normalized.equals(name)) {
                    continue;
                }
                if (normalized.equals(home)) {
                    return Outcome.ONE;
                }
                if (away.isPresent() && normalized.equals(away.get())) {
                    return Outcome.TWO;
                }
            }
        }
        return null;
    }

    private Optional<String> determineAwayTeam(TheOddsApiEventDto event) {
        List<String> teams = event.teams();
        if (teams == null || teams.isEmpty()) {
            return Optional.empty();
        }
        String home = event.homeTeam();
        return teams.stream()
            .filter(Objects::nonNull)
            .filter(team -> home == null || !team.equalsIgnoreCase(home))
            .findFirst();
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private URI buildOddsUri(Duration horizon, String leagueKey) {
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromUri(properties.baseUrl())
            .path(properties.oddsEndpoint())
            .queryParam("apiKey", properties.apiKey())
            .queryParam("regions", "eu")
            .queryParam("markets", "h2h")
            .queryParam("oddsFormat", "decimal");

        Optional.ofNullable(horizon)
            .filter(duration -> !duration.isNegative())
            .map(Duration::abs)
            .map(duration -> OffsetDateTime.now(ZoneOffset.UTC).plus(duration))
            .ifPresent(end -> builder
                .queryParam("dateFormat", "iso8601")
                .queryParam("to", end.toString()));

        String resolvedLeagueKey = Optional.ofNullable(leagueKey)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .orElse(properties.defaultSport());

        return builder
            .buildAndExpand(resolvedLeagueKey)
            .toUri();
    }
}
