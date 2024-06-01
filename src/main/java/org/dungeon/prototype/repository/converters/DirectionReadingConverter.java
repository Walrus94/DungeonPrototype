package org.dungeon.prototype.repository.converters;

import org.dungeon.prototype.model.Direction;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class DirectionReadingConverter implements Converter<String, Direction> {
    @Override
    public Direction convert(@NotNull String source) {
        return Direction.fromValue(source);
    }
}
