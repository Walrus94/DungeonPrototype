package org.dungeon.prototype.repository.mongo.converters;

import jakarta.validation.constraints.NotNull;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class PlayerAttributeReadingConverter implements Converter<String, PlayerAttribute> {
    @Override
    public PlayerAttribute convert(@NotNull String source) {
        return PlayerAttribute.fromValue(source);
    }
}
