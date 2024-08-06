package org.dungeon.prototype.repository.converters;

import org.dungeon.prototype.model.effect.attributes.MonsterEffectAttribute;
import org.springframework.core.convert.converter.Converter;

public class MonsterEffectAttributeWritingConverter implements Converter<MonsterEffectAttribute, String> {
    @Override
    public String convert(MonsterEffectAttribute source) {
        return source.getValue();
    }
}
