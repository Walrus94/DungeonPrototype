package org.dungeon.prototype.repository.converters;

import org.dungeon.prototype.model.effect.attributes.MonsterEffectAttribute;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class MonsterEffectAttributeReadingConverter implements Converter<String, MonsterEffectAttribute> {
    @Override
    public MonsterEffectAttribute convert(@NotNull String source) {
        return MonsterEffectAttribute.fromValue(source);
    }
}
