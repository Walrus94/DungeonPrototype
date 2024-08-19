package org.dungeon.prototype.repository.converters;

import jakarta.validation.constraints.NotNull;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class WearableTypeReadingConverter implements Converter<String, WearableType> {

    @Override
    public WearableType convert(@NotNull String source) {
        return WearableType.fromValue(source);
    }
}
