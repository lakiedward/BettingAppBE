package com.valuebet.backend.web.dto;

import com.valuebet.backend.domain.model.BetResult;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BetResponseDto(
    Long id,
    UUID eventId,
    MarketType marketType,
    Outcome outcome,
    BigDecimal stake,
    BigDecimal oddsTaken,
    Long bookmakerId,
    BetResult result,
    Instant createdAt
) {
}
