package org.dungeon.prototype.repository.mongo.converters;

import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class WearableTypeWritingConverter implements Converter<WearableType, String> {
    @Override
    public String convert(WearableType source) {
        return source.getValue();
    }
}
