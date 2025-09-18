package com.valuebet.backend.integration.odds.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record TheOddsApiOutcomeDto(
    @JsonProperty("name")
    String name,
    @JsonProperty("price")
    BigDecimal price
) {
}