package org.dungeon.prototype.repository.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.dungeon.prototype.model.Point;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class PointWritingConverter implements Converter<Point, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @SneakyThrows
    @Override
    public String convert(@NotNull Point source) {
        return OBJECT_MAPPER.writeValueAsString(source);
    }
}
