package org.dungeon.prototype.repository.converters;

import org.dungeon.prototype.model.player.PlayerAttribute;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class AttributeReadingConverter implements Converter<String, PlayerAttribute> {
    @Override
    public PlayerAttribute convert(@NotNull String source) {
        return PlayerAttribute.fromValue(source);
    }
}
