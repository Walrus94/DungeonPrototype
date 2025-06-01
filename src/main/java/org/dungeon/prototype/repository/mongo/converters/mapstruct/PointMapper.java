package org.dungeon.prototype.repository.mongo.converters.mapstruct;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.dungeon.prototype.model.Point;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PointMapper {
    PointMapper INSTANCE = Mappers.getMapper(PointMapper.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    TypeReference<Point> TYPE_REFERENCE = new TypeReference<>(){};


    @SneakyThrows
    default Point mapToPoint(String source) {
        return OBJECT_MAPPER.readValue(source, TYPE_REFERENCE);
    }

    @SneakyThrows
    default String mapToDocument(Point source) {
        return OBJECT_MAPPER.writeValueAsString(source);
    }
}
