package com.valuebet.backend.integration.odds;

import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ProviderOddsDto(
    String league,
    List<String> teams,
    OffsetDateTime startTime,
    MarketType marketType,
    Outcome outcome,
    String bookmakerKey,
    BigDecimal decimalOdds
) {
}
