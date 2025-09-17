package com.valuebet.backend.web.dto;

import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;

public record EventOutcomeOddsDto(
    MarketType marketType,
    Outcome outcome,
    String bestBookmaker,
    double bestOdds,
    Double closingOdds,
    double marketProbability,
    double trueProbability
) {
}
