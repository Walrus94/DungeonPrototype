package org.dungeon.prototype.repository.mongo.converters.mapstruct;

import org.dungeon.prototype.model.document.item.EffectDocument;
import org.dungeon.prototype.model.effect.AdditionEffect;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.ExpirableAdditionEffect;
import org.dungeon.prototype.model.effect.ExpirableEffect;
import org.dungeon.prototype.model.effect.ExpirableMultiplicationEffect;
import org.dungeon.prototype.model.effect.MultiplicationEffect;
import org.dungeon.prototype.model.effect.PermanentAdditionEffect;
import org.dungeon.prototype.model.effect.PermanentMultiplicationEffect;
import org.dungeon.prototype.model.effect.attributes.Action;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = WeightMapper.class)
public interface EffectMapper {
    EffectMapper INSTANCE = Mappers.getMapper(EffectMapper.class);

    @Mappings({
            @Mapping(target = "turnsLeft", source = "effect", qualifiedByName = "mapTurnsLeftToDocument"),
            @Mapping(target = "amount", source = "effect", qualifiedByName = "mapAmountToDocument"),
            @Mapping(target = "multiplier", source = "effect", qualifiedByName = "mapMultiplierToDocument"),
            @Mapping(target = "isAccumulated", source = "effect", qualifiedByName = "mapIsAccumulatedToDocument"),
            @Mapping(target = "isPermanent", expression = "java(effect instanceof PermanentMultiplicationEffect)"),
    })
    EffectDocument mapToDocument(Effect effect);

    default Effect mapToEffect(EffectDocument document) {
        if (document.getAction().equals(Action.MULTIPLY)) {
            return mapToMultiplicationEffect(document);
        } else {
            return mapToAdditionEffect(document);
        }
    }

    default AdditionEffect mapToAdditionEffect(EffectDocument document) {
        if (document.getIsPermanent()) {
            return mapToPermanentAdditionEffect(document);
        } else {
            return mapToExpirableAdditionEffect(document);
        }
    }

    default MultiplicationEffect mapToMultiplicationEffect(EffectDocument document) {
        if (document.getIsPermanent()) {
            return mapToPermanentMultiplicationEffect(document);
        } else {
            return mapToExpirableMultiplicationEffect(document);
        }
    }

    PermanentAdditionEffect mapToPermanentAdditionEffect(EffectDocument document);

    ExpirableAdditionEffect mapToExpirableAdditionEffect(EffectDocument document);

    PermanentMultiplicationEffect mapToPermanentMultiplicationEffect(EffectDocument document);

    ExpirableMultiplicationEffect mapToExpirableMultiplicationEffect(EffectDocument document);

    @Named("mapTurnsLeftToDocument")
    default Integer mapTurnsLeftToDocument(Effect effect) {
        if (effect instanceof ExpirableEffect) {
            return ((ExpirableEffect) effect).getTurnsLeft();
        } else {
            return null;
        }
    }

    @Named("mapIsAccumulatedToDocument")
    default Boolean mapIsAccumulatedToDocument(Effect effect) {
        if (effect instanceof ExpirableEffect) {
            return ((ExpirableEffect) effect).isAccumulated();
        } else {
            return null;
        }
    }

    @Named("mapAmountToDocument")
    default Integer mapAmountToDocument(Effect effect) {
        if (effect instanceof AdditionEffect) {
            return ((AdditionEffect) effect).getAmount();
        } else {
            return null;
        }
    }

    @Named("mapMultiplierToDocument")
    default Double mapMultiplierToDocument(Effect effect) {
        if (effect instanceof MultiplicationEffect) {
            return ((MultiplicationEffect) effect).getMultiplier();
        } else {
            return null;
        }
    }

}
