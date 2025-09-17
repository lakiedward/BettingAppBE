package com.valuebet.backend.web.mapper;

import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.web.dto.EventDto;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventDto toDto(Event event);

    List<EventDto> toDtoList(List<Event> events);
}
