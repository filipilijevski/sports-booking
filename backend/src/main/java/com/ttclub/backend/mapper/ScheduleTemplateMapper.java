package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.model.ScheduleTemplate;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ScheduleTemplateMapper {

    ScheduleTemplateDto toDto(ScheduleTemplate entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "coach", ignore = true)          // set manually
    @Mapping(target = "active", ignore = true)
    ScheduleTemplate toEntity(UpsertScheduleTemplateDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UpsertScheduleTemplateDto dto,
                             @MappingTarget ScheduleTemplate entity);
}
