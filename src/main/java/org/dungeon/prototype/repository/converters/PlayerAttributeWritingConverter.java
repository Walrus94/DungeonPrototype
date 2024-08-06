package org.dungeon.prototype.repository.converters;

import org.dungeon.prototype.model.player.PlayerAttribute;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class PlayerAttributeWritingConverter implements Converter<PlayerAttribute, String> {
    @Override
    public String convert(PlayerAttribute source) {
        return source.getValue();
    }
}
