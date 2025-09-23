package com.valuebet.backend.integration.odds;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;

class MockOddsProviderClientTest {

    private MockOddsProviderClient client;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        client = new MockOddsProviderClient(objectMapper);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:mock/odds-*.json");
        ReflectionTestUtils.setField(client, "oddsResources", resources);
    }

    @Test
    void shouldMergeOddsAcrossTrackedLeagues() {
        List<ProviderOddsDto> odds = client.fetchUpcomingOdds(Duration.ofDays(1000));

        assertThat(odds).isNotEmpty();
        Set<String> leagues = odds.stream().map(ProviderOddsDto::league).collect(Collectors.toSet());
        assertThat(leagues)
            .contains("English Premier League", "La Liga", "Serie A", "Bundesliga", "Ligue 1");
    }

    @Test
    void shouldRespectCachingBetweenInvocations() {
        List<ProviderOddsDto> firstCall = client.fetchUpcomingOdds(null);
        List<ProviderOddsDto> secondCall = client.fetchUpcomingOdds(null);

        assertThat(firstCall).isEqualTo(secondCall);
    }
}
