package org.dungeon.prototype.repository.converters;

import org.dungeon.prototype.model.player.Attribute;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class AttributeWritingConverter implements Converter<Attribute, String> {
    @Override
    public String convert(Attribute source) {
        return source.getValue();
    }
}
