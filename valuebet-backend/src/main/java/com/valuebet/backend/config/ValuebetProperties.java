package com.valuebet.backend.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "valuebet")
public record ValuebetProperties(Duration ingestionHorizon, ClosingLineProperties closingLine) {

    public ValuebetProperties {
        ingestionHorizon = ingestionHorizon == null ? Duration.ofHours(12) : ingestionHorizon;
        closingLine = closingLine == null ? new ClosingLineProperties(List.of()) : closingLine;
    }

    public record ClosingLineProperties(List<String> priorityBookmakers) {

        public ClosingLineProperties {
            priorityBookmakers = priorityBookmakers == null ? List.of() : List.copyOf(priorityBookmakers);
        }
    }
}
