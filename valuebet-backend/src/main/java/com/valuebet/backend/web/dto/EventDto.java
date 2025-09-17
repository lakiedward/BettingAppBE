package com.valuebet.backend.web.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EventDto(
    UUID id,
    String externalId,
    String name,
    String competition,
    OffsetDateTime startTime,
    Instant createdAt,
    Instant updatedAt
) {
}
