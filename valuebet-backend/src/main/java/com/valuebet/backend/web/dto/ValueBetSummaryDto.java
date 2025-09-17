package com.valuebet.backend.web.dto;

import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ValueBetSummaryDto(
    UUID eventId,
    String league,
    String home,
    String away,
    OffsetDateTime startTime,
    MarketType marketType,
    Outcome outcome,
    String bestBookmaker,
    double bestOdds,
    double pTrue,
    double ev,
    double betDownTo,
    Instant computedAt
) {
}
