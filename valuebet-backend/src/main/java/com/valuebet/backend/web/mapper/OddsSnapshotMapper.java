package com.valuebet.backend.web.mapper;

import com.valuebet.backend.domain.model.OddsSnapshot;
import com.valuebet.backend.web.dto.OddsSnapshotDto;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OddsSnapshotMapper {

    @Mapping(target = "eventId", source = "event.id")
    OddsSnapshotDto toDto(OddsSnapshot snapshot);

    List<OddsSnapshotDto> toDtoList(List<OddsSnapshot> snapshots);
}
