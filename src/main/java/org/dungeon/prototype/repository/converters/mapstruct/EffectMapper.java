package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.item.EffectDocument;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.ExpirableEffect;
import org.dungeon.prototype.model.effect.PermanentEffect;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EffectMapper {
    EffectMapper INSTANCE = Mappers.getMapper(EffectMapper.class);

    @Mappings({
            @Mapping(target = "turnsLasts", source = "effect", qualifiedByName = "mapTurnsLastsToDocument"),
            @Mapping(target = "isAccumulated", source = "effect", qualifiedByName = "mapIsAccumulatedToDocument"),
            @Mapping(target = "baseAmount", source = "effect", qualifiedByName = "mapBaseAmountToDocument"),
            @Mapping(target = "isPermanent", expression = "java(effect instanceof PermanentEffect)")
    })
    EffectDocument mapToDocument(Effect effect);

    default Effect mapToEffect(EffectDocument document) {
        if (document.getIsPermanent()) {
            return mapToPermanentEffect(document);
        } else {
            return mapToExpirableEffect(document);
        }
    }

    ExpirableEffect mapToExpirableEffect(EffectDocument document);

    PermanentEffect mapToPermanentEffect(EffectDocument document);

    @Named("mapTurnsLastsToDocument")
    default Integer mapTurnsLastsToDocument(Effect effect) {
        if (effect instanceof ExpirableEffect) {
            return ((ExpirableEffect) effect).getTurnsLasts();
        } else {
            return null;
        }
    }

    @Named("mapIsAccumulatedToDocument")
    default Boolean mapIsAccumulatedToDocument(Effect effect) {
        if (effect instanceof ExpirableEffect) {
            return ((ExpirableEffect) effect).getIsAccumulated();
        } else {
            return null;
        }
    }
    @Named("mapBaseAmountToDocument")
    default Integer mapBaseAmountToDocument(Effect effect) {
        if (effect instanceof ExpirableEffect) {
            return ((ExpirableEffect) effect).getBaseAmount();
        } else {
            return null;
        }
    }

}
