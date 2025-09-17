package com.valuebet.backend.web.dto;

import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ValueOpportunityDto(
    Long id,
    UUID eventId,
    MarketType marketType,
    Outcome outcome,
    BigDecimal line,
    BigDecimal odds,
    BigDecimal trueProbability,
    BigDecimal edge,
    String source,
    Instant createdAt
) {
}
