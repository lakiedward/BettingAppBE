package com.valuebet.backend.integration.odds.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record TheOddsApiBookmakerDto(
    @JsonProperty("key")
    String key,
    @JsonProperty("title")
    String title,
    @JsonProperty("last_update")
    OffsetDateTime lastUpdate,
    @JsonProperty("markets")
    List<TheOddsApiMarketDto> markets
) {
}