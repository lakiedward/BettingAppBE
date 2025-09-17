package com.valuebet.backend.web.mapper;

import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.domain.model.ValueOpportunity;
import com.valuebet.backend.web.dto.ValueBetSummaryDto;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ValueBetSummaryMapper {

    public ValueBetSummaryDto toDto(ValueOpportunity opportunity) {
        Event event = opportunity.getEvent();
        UUID eventId = event != null ? event.getId() : null;
        String home = event != null ? event.getHomeTeam() : null;
        String away = event != null ? event.getAwayTeam() : null;
        String league = event != null ? event.getCompetition() : null;

        return new ValueBetSummaryDto(
            eventId,
            league,
            home,
            away,
            event != null ? event.getStartTime() : null,
            opportunity.getMarketType(),
            opportunity.getOutcome(),
            opportunity.getBookmaker() != null ? opportunity.getBookmaker().getExternalKey() : null,
            toDouble(opportunity.getOdds()),
            toDouble(opportunity.getTrueProbability()),
            toDouble(opportunity.getEdge()),
            toDouble(opportunity.getMinOddsEv0()),
            opportunity.getCreatedAt()
        );
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0.0d : value.doubleValue();
    }
}
