package org.dungeon.prototype.repository.converters;

import org.dungeon.prototype.model.effect.attributes.PlayerEffectAttribute;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class PlayerEffectAttributeWritingConverter implements Converter<PlayerEffectAttribute, String> {
    @Override
    public String convert(PlayerEffectAttribute source) {
        return source.getValue();
    }
}
