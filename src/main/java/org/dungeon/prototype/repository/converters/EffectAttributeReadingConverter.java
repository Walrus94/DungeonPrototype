package org.dungeon.prototype.repository.converters;

import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.model.effect.attributes.MonsterEffectAttribute;
import org.dungeon.prototype.model.effect.attributes.PlayerEffectAttribute;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class EffectAttributeReadingConverter implements Converter<String, EffectAttribute> {
    @Override
    public EffectAttribute convert(@NotNull String source) {
        try {
            return MonsterEffectAttribute.fromValue(source);
        } catch (IllegalArgumentException e) {
            return PlayerEffectAttribute.fromValue(source);
        }
    }
}