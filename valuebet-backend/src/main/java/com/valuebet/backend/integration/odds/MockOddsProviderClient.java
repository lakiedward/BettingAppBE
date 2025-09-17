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
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
@Profile("dev")
@RequiredArgsConstructor
public class MockOddsProviderClient implements OddsProviderClient {

    private final ObjectMapper objectMapper;

    @Value("classpath:mock/odds.json")
    private Resource oddsResource;

    private final AtomicReference<List<MockEvent>> cache = new AtomicReference<>();

    @Override
    public List<ProviderOddsDto> fetchUpcomingOdds(Duration horizon) {
        List<MockEvent> events = loadFixtures();
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime cutoff = horizon == null ? null : now.plus(horizon);

        return events.stream()
            .filter(event -> cutoff == null || !event.startTime().isAfter(cutoff))
            .limit(20)
            .flatMap(MockEvent::toOddsStream)
            .collect(Collectors.toList());
    }

    private List<MockEvent> loadFixtures() {
        List<MockEvent> cached = cache.get();
        if (cached != null) {
            return cached;
        }
        try {
            List<MockEvent> loaded = objectMapper.readValue(
                oddsResource.getInputStream(),
                new TypeReference<List<MockEvent>>() { }
            );
            cache.compareAndSet(null, loaded);
            return loaded;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mock odds fixture", e);
        }
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
