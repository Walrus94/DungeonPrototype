package org.dungeon.prototype.repository.mongo.converters;

import org.dungeon.prototype.model.Direction;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class DirectionWritingConverter implements Converter<Direction, String> {
    @Override
    public String convert(Direction source) {
        return source.name();
    }
}
