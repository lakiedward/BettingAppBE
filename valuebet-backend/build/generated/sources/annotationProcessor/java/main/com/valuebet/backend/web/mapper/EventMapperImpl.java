package com.valuebet.backend.web.mapper;

import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.web.dto.EventDto;
import java.time.Instant;
import java.time.OffsetDateTime;
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
public class EventMapperImpl implements EventMapper {

    @Override
    public EventDto toDto(Event event) {
        if ( event == null ) {
            return null;
        }

        UUID id = null;
        String externalId = null;
        String name = null;
        String competition = null;
        OffsetDateTime startTime = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        id = event.getId();
        externalId = event.getExternalId();
        name = event.getName();
        competition = event.getCompetition();
        startTime = event.getStartTime();
        createdAt = event.getCreatedAt();
        updatedAt = event.getUpdatedAt();

        EventDto eventDto = new EventDto( id, externalId, name, competition, startTime, createdAt, updatedAt );

        return eventDto;
    }

    @Override
    public List<EventDto> toDtoList(List<Event> events) {
        if ( events == null ) {
            return null;
        }

        List<EventDto> list = new ArrayList<EventDto>( events.size() );
        for ( Event event : events ) {
            list.add( toDto( event ) );
        }

        return list;
    }
}
