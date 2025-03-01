package org.dungeon.prototype.repository.mongo.converters;

import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class EffectAttributeWritingConverter implements Converter<EffectAttribute, String> {
    @Override
    public String convert(EffectAttribute source) {
        return source.getValue();
    }
}
