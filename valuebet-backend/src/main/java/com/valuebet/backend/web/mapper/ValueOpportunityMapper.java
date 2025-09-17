package com.valuebet.backend.web.mapper;

import com.valuebet.backend.domain.model.ValueOpportunity;
import com.valuebet.backend.web.dto.ValueOpportunityDto;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ValueOpportunityMapper {

    @Mapping(target = "eventId", source = "event.id")
    ValueOpportunityDto toDto(ValueOpportunity opportunity);

    List<ValueOpportunityDto> toDtoList(List<ValueOpportunity> opportunities);
}
