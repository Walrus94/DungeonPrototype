package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.item.EffectDocument;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.effect.Effect;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EffectMapper {
    EffectMapper INSTANCE = Mappers.getMapper(EffectMapper.class);

    @Mapping(source = "applicableTo", target = "applicableTo", qualifiedByName = "classToString")
    EffectDocument mapToDocument(Effect effect);

    @Mapping(source = "applicableTo", target = "applicableTo", qualifiedByName = "stringToClass")
    Effect mapToEffect(EffectDocument document);

    @Named("classToString")
    default String classToString(Class<? extends Item> clazz) {
        return clazz != null ? clazz.getName() : null;
    }

    @Named("stringToClass")
    default Class<? extends Item> stringToClass(String className) {
        try {
            return className != null ? (Class<? extends Item>)  Class.forName(className) : null;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }
}
