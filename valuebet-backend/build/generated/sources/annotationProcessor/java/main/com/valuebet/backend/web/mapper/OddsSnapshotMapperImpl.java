package com.valuebet.backend.web.mapper;

import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.OddsSnapshot;
import com.valuebet.backend.domain.model.Outcome;
import com.valuebet.backend.web.dto.OddsSnapshotDto;
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
public class OddsSnapshotMapperImpl implements OddsSnapshotMapper {

    @Override
    public OddsSnapshotDto toDto(OddsSnapshot snapshot) {
        if ( snapshot == null ) {
            return null;
        }

        UUID eventId = null;
        Long id = null;
        MarketType marketType = null;
        Outcome outcome = null;
        BigDecimal line = null;
        String bookmaker = null;
        BigDecimal odds = null;
        BigDecimal impliedProbability = null;
        Instant capturedAt = null;
        Instant createdAt = null;

        eventId = snapshotEventId( snapshot );
        id = snapshot.getId();
        marketType = snapshot.getMarketType();
        outcome = snapshot.getOutcome();
        line = snapshot.getLine();
        bookmaker = snapshot.getBookmaker();
        odds = snapshot.getOdds();
        impliedProbability = snapshot.getImpliedProbability();
        capturedAt = snapshot.getCapturedAt();
        createdAt = snapshot.getCreatedAt();

        OddsSnapshotDto oddsSnapshotDto = new OddsSnapshotDto( id, eventId, marketType, outcome, line, bookmaker, odds, impliedProbability, capturedAt, createdAt );

        return oddsSnapshotDto;
    }

    @Override
    public List<OddsSnapshotDto> toDtoList(List<OddsSnapshot> snapshots) {
        if ( snapshots == null ) {
            return null;
        }

        List<OddsSnapshotDto> list = new ArrayList<OddsSnapshotDto>( snapshots.size() );
        for ( OddsSnapshot oddsSnapshot : snapshots ) {
            list.add( toDto( oddsSnapshot ) );
        }

        return list;
    }

    private UUID snapshotEventId(OddsSnapshot oddsSnapshot) {
        if ( oddsSnapshot == null ) {
            return null;
        }
        Event event = oddsSnapshot.getEvent();
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
