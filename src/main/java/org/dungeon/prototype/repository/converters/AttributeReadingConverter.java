package org.dungeon.prototype.repository.converters;

import org.dungeon.prototype.model.player.Attribute;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class AttributeReadingConverter implements Converter<String, Attribute> {
    @Override
    public Attribute convert(@NotNull String source) {
        return Attribute.fromValue(source);
    }
}
