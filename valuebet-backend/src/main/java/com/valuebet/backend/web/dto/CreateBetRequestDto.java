package com.valuebet.backend.web.dto;

import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateBetRequestDto(
    UUID eventId,
    MarketType marketType,
    Outcome outcome,
    BigDecimal stake,
    BigDecimal oddsTaken,
    Long bookmakerId
) {
}
