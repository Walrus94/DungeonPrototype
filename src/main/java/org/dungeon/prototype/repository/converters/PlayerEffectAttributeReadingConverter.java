package org.dungeon.prototype.repository.converters;

import org.dungeon.prototype.model.effect.attributes.PlayerEffectAttribute;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class PlayerEffectAttributeReadingConverter implements Converter<String, PlayerEffectAttribute> {

    @Override
    public PlayerEffectAttribute convert(@NotNull String source) {
        return PlayerEffectAttribute.fromValue(source);
    }
}
