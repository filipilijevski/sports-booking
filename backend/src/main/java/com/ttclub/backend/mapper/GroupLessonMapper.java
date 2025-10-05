package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.model.GroupLesson;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupLessonMapper {

    /* entity to DTO */
    @Mapping(target = "coachId", source = "coach.id")
    GroupLessonDto toDto(GroupLesson entity);

    List<GroupLessonDto> toDto(List<GroupLesson> entities);

    /* DTO to entity (create) */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "coach", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    GroupLesson toEntity(UpsertGroupLessonDto dto);

    /* DTO to entity (update in place) */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget GroupLesson entity, UpsertGroupLessonDto dto);
}
