package org.dungeon.prototype.repository.mongo.converters;

import jakarta.validation.constraints.NotNull;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class EffectAttributeReadingConverter implements Converter<String, EffectAttribute> {
    @Override
    public EffectAttribute convert(@NotNull String source) {
        return EffectAttribute.fromValue(source);
    }
}
