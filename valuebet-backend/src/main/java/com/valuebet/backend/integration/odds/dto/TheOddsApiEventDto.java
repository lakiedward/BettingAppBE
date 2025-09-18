package com.valuebet.backend.integration.odds.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record TheOddsApiEventDto(
    @JsonProperty("id")
    String id,
    @JsonProperty("sport_key")
    String sportKey,
    @JsonProperty("sport_title")
    String sportTitle,
    @JsonProperty("commence_time")
    OffsetDateTime commenceTime,
    @JsonProperty("teams")
    List<String> teams,
    @JsonProperty("home_team")
    String homeTeam,
    @JsonProperty("bookmakers")
    List<TheOddsApiBookmakerDto> bookmakers
) {
}