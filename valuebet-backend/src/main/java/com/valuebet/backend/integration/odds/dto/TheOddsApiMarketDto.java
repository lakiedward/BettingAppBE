package com.valuebet.backend.integration.odds.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TheOddsApiMarketDto(
    @JsonProperty("key")
    String key,
    @JsonProperty("outcomes")
    List<TheOddsApiOutcomeDto> outcomes
) {
}