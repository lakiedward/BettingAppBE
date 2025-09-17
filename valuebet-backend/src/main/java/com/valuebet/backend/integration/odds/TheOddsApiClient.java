package com.valuebet.backend.integration.odds;

import com.valuebet.backend.integration.odds.config.TheOddsApiProperties;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("!dev")
public class TheOddsApiClient implements OddsProviderClient {

    private static final Logger log = LoggerFactory.getLogger(TheOddsApiClient.class);

    private final RestTemplate restTemplate;
    private final TheOddsApiProperties properties;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public TheOddsApiClient(RestTemplateBuilder restTemplateBuilder,
                            TheOddsApiProperties properties,
                            RetryRegistry retryRegistry,
                            TimeLimiterRegistry timeLimiterRegistry) {
        this.properties = properties;
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
        return retry.executeSupplier(() ->
            timeLimiter.executeFutureSupplier(() ->
                CompletableFuture.supplyAsync(() -> doFetchUpcomingOdds(horizon))
            )
        );
    }

    private List<ProviderOddsDto> doFetchUpcomingOdds(Duration horizon) {
        URI uri = buildOddsUri(horizon);
        log.debug("Fetching odds from {}", uri);
        // TODO: Integrate with The Odds API response model once the API key is available.
        restTemplate.getForEntity(uri, String.class);
        return Collections.emptyList();
    }

    private URI buildOddsUri(Duration horizon) {
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromUri(properties.baseUrl())
            .path(properties.oddsEndpoint())
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

        return builder
            .buildAndExpand(properties.defaultSport())
            .toUri();
    }
}
