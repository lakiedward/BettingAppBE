package com.valuebet.backend.web.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EventOddsDto(
    UUID eventId,
    String league,
    String home,
    String away,
    OffsetDateTime startTime,
    List<EventOutcomeOddsDto> outcomes
) {
}
