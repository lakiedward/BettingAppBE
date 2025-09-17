package com.valuebet.backend.service;

import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ProbabilityService {

    public Map<Outcome, Double> estimateTrueProb(Event event,
                                                 MarketType market,
                                                 Map<Outcome, Double> pMarketNoVig) {
        if (pMarketNoVig == null || pMarketNoVig.isEmpty()) {
            return Map.of();
        }
        EnumMap<Outcome, Double> copy = new EnumMap<>(Outcome.class);
        pMarketNoVig.forEach(copy::put);
        return copy;
    }
}
