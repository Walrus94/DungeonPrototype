package org.dungeon.prototype.repository.mongo.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import org.dungeon.prototype.model.Point;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class PointReadingConverter implements Converter<String, Point> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Point> TYPE_REFERENCE = new TypeReference<>(){};

    @SneakyThrows
    @Override
    public Point convert(@NotNull String source) {
        return OBJECT_MAPPER.readValue(source, TYPE_REFERENCE);
    }
}
