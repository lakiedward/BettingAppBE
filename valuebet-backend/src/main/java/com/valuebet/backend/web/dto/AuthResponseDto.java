package com.valuebet.backend.web.dto;

import java.time.Instant;

public record AuthResponseDto(String token, Instant expiresAt) {
}
