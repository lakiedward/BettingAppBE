package com.valuebet.backend.web.dto;

public record ClvSummaryDto(
    double averageClv,
    double roi,
    int betsCount
) {
}
