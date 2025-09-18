package com.valuebet.backend.web.mapper;

import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import com.valuebet.backend.domain.model.ValueOpportunity;
import com.valuebet.backend.web.dto.ValueOpportunityDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-09-18T16:52:26+0300",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.3.jar, environment: Java 17.0.16 (Eclipse Adoptium)"
)
@Component
public class ValueOpportunityMapperImpl implements ValueOpportunityMapper {

    @Override
    public ValueOpportunityDto toDto(ValueOpportunity opportunity) {
        if ( opportunity == null ) {
            return null;
        }

        UUID eventId = null;
        Long id = null;
        MarketType marketType = null;
        Outcome outcome = null;
        BigDecimal line = null;
        BigDecimal odds = null;
        BigDecimal trueProbability = null;
        BigDecimal edge = null;
        String source = null;
        Instant createdAt = null;

        eventId = opportunityEventId( opportunity );
        id = opportunity.getId();
        marketType = opportunity.getMarketType();
        outcome = opportunity.getOutcome();
        line = opportunity.getLine();
        odds = opportunity.getOdds();
        trueProbability = opportunity.getTrueProbability();
        edge = opportunity.getEdge();
        source = opportunity.getSource();
        createdAt = opportunity.getCreatedAt();

        ValueOpportunityDto valueOpportunityDto = new ValueOpportunityDto( id, eventId, marketType, outcome, line, odds, trueProbability, edge, source, createdAt );

        return valueOpportunityDto;
    }

    @Override
    public List<ValueOpportunityDto> toDtoList(List<ValueOpportunity> opportunities) {
        if ( opportunities == null ) {
            return null;
        }

        List<ValueOpportunityDto> list = new ArrayList<ValueOpportunityDto>( opportunities.size() );
        for ( ValueOpportunity valueOpportunity : opportunities ) {
            list.add( toDto( valueOpportunity ) );
        }

        return list;
    }

    private UUID opportunityEventId(ValueOpportunity valueOpportunity) {
        if ( valueOpportunity == null ) {
            return null;
        }
        Event event = valueOpportunity.getEvent();
        if ( event == null ) {
            return null;
        }
        UUID id = event.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
