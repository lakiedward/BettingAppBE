package com.valuebet.backend.service;

import com.valuebet.backend.domain.model.Bookmaker;
import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import com.valuebet.backend.domain.model.ValueOpportunity;
import com.valuebet.backend.domain.repository.BookmakerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValueDetectionService {

    private static final double MIN_EV_THRESHOLD_SECONDARY = 0.02d;
    private static final double DEFAULT_KELLY_FRACTION = 0.5d;

    private final BookmakerRepository bookmakerRepository;

    public ValueOpportunity detect(Event event,
                                   MarketType market,
                                   Outcome outcome,
                                   double bestOdds,
                                   long bestBookmakerId,
                                   double pTrue) {
        Bookmaker bookmaker = bookmakerRepository.getReferenceById(bestBookmakerId);
        double edgeValue = bestOdds * pTrue - 1.0d;

        double minOddsEv0 = betDownTo(pTrue, 0.0d);
        double minOddsEv2 = betDownTo(pTrue, MIN_EV_THRESHOLD_SECONDARY);
        double kelly = kellyFraction(bestOdds, pTrue, DEFAULT_KELLY_FRACTION);

        ValueOpportunity valueOpportunity = ValueOpportunity.builder()
            .event(event)
            .marketType(market)
            .outcome(outcome)
            .odds(scaleOdds(bestOdds))
            .trueProbability(scaleProbability(pTrue))
            .edge(scaleEdge(edgeValue))
            .bookmaker(bookmaker)
            .source(bookmaker.getExternalKey())
            .build();

        valueOpportunity.setMinOddsEv0(scaleOdds(minOddsEv0));
        valueOpportunity.setMinOddsEv2(scaleOdds(minOddsEv2));
        valueOpportunity.setKellyFraction(scaleProbability(kelly));
        return valueOpportunity;
    }

    public double betDownTo(double pTrue, double minEv) {
        if (pTrue <= 0.0d) {
            return Double.POSITIVE_INFINITY;
        }
        return (1.0d + minEv) / pTrue;
    }

    public double kellyFraction(double odds, double pTrue, double fraction) {
        if (fraction <= 0.0d || odds <= 1.0d || pTrue <= 0.0d) {
            return 0.0d;
        }
        double numerator = odds * pTrue - 1.0d;
        double denominator = odds - 1.0d;
        if (denominator <= 0.0d || numerator <= 0.0d) {
            return 0.0d;
        }
        return fraction * (numerator / denominator);
    }

    private BigDecimal scaleOdds(double odds) {
        if (Double.isInfinite(odds) || Double.isNaN(odds)) {
            return null;
        }
        return BigDecimal.valueOf(odds).setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleProbability(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleEdge(double edge) {
        return BigDecimal.valueOf(edge).setScale(4, RoundingMode.HALF_UP);
    }
}
