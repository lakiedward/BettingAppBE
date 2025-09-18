package com.valuebet.backend.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "valuebet")
public record ValuebetProperties(
    Duration ingestionHorizon,
    ClosingLineProperties closingLine,
    FilterProperties filter
) {

    public ValuebetProperties {
        ingestionHorizon = ingestionHorizon == null ? Duration.ofHours(12) : ingestionHorizon;
        closingLine = closingLine == null ? new ClosingLineProperties(List.of()) : closingLine;
        filter = filter == null ? FilterProperties.defaults() : filter;
    }

    public record ClosingLineProperties(List<String> priorityBookmakers) {

        public ClosingLineProperties {
            priorityBookmakers = priorityBookmakers == null ? List.of() : List.copyOf(priorityBookmakers);
        }
    }

    public record FilterProperties(List<String> trackedLeagues,
                                   Duration timeWindow,
                                   List<String> majorCountries) {

        public FilterProperties {
            trackedLeagues = trackedLeagues == null ? List.of() : List.copyOf(trackedLeagues);
            timeWindow = timeWindow == null ? Duration.ofDays(2) : timeWindow;
            majorCountries = majorCountries == null ? List.of() : List.copyOf(majorCountries);
        }

        public static FilterProperties defaults() {
            return new FilterProperties(List.of(), Duration.ofDays(2), List.of());
        }
    }
}
