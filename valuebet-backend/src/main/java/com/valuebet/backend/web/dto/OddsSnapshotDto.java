package com.valuebet.backend.web.dto;

import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OddsSnapshotDto(
    Long id,
    UUID eventId,
    MarketType marketType,
    Outcome outcome,
    BigDecimal line,
    String bookmaker,
    BigDecimal odds,
    BigDecimal impliedProbability,
    Instant capturedAt,
    Instant createdAt
) {
}
