package com.valuebet.backend.integration.odds.config;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.the-odds-api")
public record TheOddsApiProperties(
    URI baseUrl,
    String sportsEndpoint,
    String oddsEndpoint,
    String defaultSport,
    String apiKey,
    Duration connectTimeout,
    Duration readTimeout,
    RetryProperties retry,
    TimeLimiterProperties timeLimiter
) {

    public TheOddsApiProperties {
        Objects.requireNonNull(baseUrl, "baseUrl is required");
        Objects.requireNonNull(sportsEndpoint, "sportsEndpoint is required");
        Objects.requireNonNull(oddsEndpoint, "oddsEndpoint is required");
        Objects.requireNonNull(defaultSport, "defaultSport is required");
        Objects.requireNonNull(apiKey, "apiKey is required");
        Objects.requireNonNull(connectTimeout, "connectTimeout is required");
        Objects.requireNonNull(readTimeout, "readTimeout is required");
        Objects.requireNonNull(retry, "retry configuration is required");
        Objects.requireNonNull(timeLimiter, "timeLimiter configuration is required");
    }

    public record RetryProperties(int maxAttempts, Duration waitDuration) {
        public RetryProperties {
            if (maxAttempts <= 0) {
                throw new IllegalArgumentException("maxAttempts must be greater than zero");
            }
            Objects.requireNonNull(waitDuration, "waitDuration is required");
        }
    }

    public record TimeLimiterProperties(Duration timeoutDuration) {
        public TimeLimiterProperties {
            Objects.requireNonNull(timeoutDuration, "timeoutDuration is required");
        }
    }
}
