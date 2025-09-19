package com.valuebet.backend.integration.odds;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("mock")
@RequiredArgsConstructor
public class MockOddsProviderClient implements OddsProviderClient {

    private final ObjectMapper objectMapper;

    @Value("classpath*:mock/odds-*.json")
    private Resource[] oddsResources;

    private final AtomicReference<Map<String, List<MockEvent>>> cache = new AtomicReference<>();

    @Override
    public List<ProviderOddsDto> fetchUpcomingOdds(Duration horizon) {
        Map<String, List<MockEvent>> eventsByLeague = loadFixtures();
        if (eventsByLeague.isEmpty()) {
            return Collections.emptyList();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime cutoff = horizon == null ? null : now.plus(horizon);

        return eventsByLeague.values().stream()
            .flatMap(List::stream)
            .filter(event -> cutoff == null || !event.startTime().isAfter(cutoff))
            .limit(20)
            .flatMap(MockEvent::toOddsStream)
            .collect(Collectors.toList());
    }

    private Map<String, List<MockEvent>> loadFixtures() {
        Map<String, List<MockEvent>> cached = cache.get();
        if (cached != null) {
            return cached;
        }
        try {
            Map<String, List<MockEvent>> loaded = new LinkedHashMap<>();
            if (oddsResources != null) {
                for (Resource resource : oddsResources) {
                    if (resource == null || !resource.exists()) {
                        continue;
                    }
                    String leagueKey = extractLeagueKey(resource);
                    List<MockEvent> events = Optional.ofNullable(objectMapper.readValue(
                            resource.getInputStream(),
                            new TypeReference<List<MockEvent>>() { }
                        ))
                        .orElseGet(List::of);
                    loaded.put(leagueKey, List.copyOf(events));
                }
            }
            Map<String, List<MockEvent>> immutable = Map.copyOf(loaded);
            if (cache.compareAndSet(null, immutable)) {
                return immutable;
            }
            return cache.get();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mock odds fixture", e);
        }
    }

    private String extractLeagueKey(Resource resource) {
        String filename = resource.getFilename();
        if (filename == null) {
            throw new IllegalStateException("Mock odds resource without filename: " + resource.getDescription());
        }
        if (filename.startsWith("odds-") && filename.endsWith(".json")) {
            return filename.substring(5, filename.length() - 5);
        }
        throw new IllegalStateException("Unsupported mock odds resource name: " + filename);
    }

    private record MockEvent(
        String league,
        String homeTeam,
        String awayTeam,
        OffsetDateTime startTime,
        List<MockBookmaker> bookmakers
    ) {

        Stream<ProviderOddsDto> toOddsStream() {
            if (bookmakers == null || bookmakers.isEmpty()) {
                return Stream.empty();
            }
            List<String> teams = List.of(homeTeam, awayTeam);
            return bookmakers.stream()
                .filter(Objects::nonNull)
                .flatMap(bookmaker -> bookmaker.toOddsStream(league, teams, startTime));
        }
    }

    private record MockBookmaker(
        String bookmakerKey,
        Map<Outcome, BigDecimal> odds
    ) {

        Stream<ProviderOddsDto> toOddsStream(String league, List<String> teams, OffsetDateTime startTime) {
            if (odds == null || odds.isEmpty()) {
                return Stream.empty();
            }
            return odds.entrySet().stream()
                .map(entry -> new ProviderOddsDto(
                    league,
                    teams,
                    startTime,
                    MarketType.ONE_X_TWO,
                    entry.getKey(),
                    bookmakerKey,
                    entry.getValue()
                ));
        }
    }
}
