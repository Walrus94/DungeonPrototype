package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.item.EffectDocument;
import org.dungeon.prototype.model.effect.DirectPlayerEffect;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.Expirable;
import org.dungeon.prototype.model.effect.ItemEffect;
import org.dungeon.prototype.model.effect.MonsterEffect;
import org.dungeon.prototype.model.effect.PlayerEffect;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.model.effect.attributes.MonsterEffectAttribute;
import org.dungeon.prototype.model.effect.attributes.PlayerEffectAttribute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

import static org.dungeon.prototype.model.effect.EffectApplicant.ITEM;
import static org.dungeon.prototype.model.effect.EffectApplicant.PLAYER;

@Mapper
public interface EffectMapper {
    EffectMapper INSTANCE = Mappers.getMapper(EffectMapper.class);

    @Mappings({
            @Mapping(target = "attribute", source = "effect", qualifiedByName = "mapAttributeToDocument"),
            @Mapping(target = "turnsLasts", source = "effect", qualifiedByName = "mapTurnsLastsToDocument"),
            @Mapping(target = "isAccumulated", source = "effect", qualifiedByName = "mapIsAccumulatedToDocument"),
            @Mapping(target = "hasFirstTurnPassed", source = "effect", qualifiedByName = "mapHasFirstTurnPassedToDocument")
    })
    EffectDocument mapToDocument(Effect effect);

    default List<EffectDocument> mapToDocuments(List<Effect> effects){
        return effects.stream().map(this::mapToDocument).collect(Collectors.toList());
    }

    default PlayerEffect mapToPlayerEffect(EffectDocument document) {
        if (document.getApplicableTo().equals(ITEM)) {
            return mapToItemEffect(document);
        } else if (document.getApplicableTo().equals(PLAYER)) {
            return mapToDirectPlayerEffect(document);
        } else {
            return null;
        }
    }

    DirectPlayerEffect mapToDirectPlayerEffect(EffectDocument document);

    ItemEffect mapToItemEffect(EffectDocument document);

    List<ItemEffect> mapToItemEffects(List<EffectDocument> document);

    MonsterEffect mapToMonsterEffect(EffectDocument document);

    @Named("mapAttributeToDocument")
    default EffectAttribute mapAttributeToDocument(Effect effect) {

        if (effect instanceof MonsterEffect) {
            return mapToMonsterAttribute(effect.getAttribute());
        } else if (effect instanceof PlayerEffect) {
            return mapToPlayerAttribute(effect.getAttribute());
        }
        return null;
    }

    @Named("mapTurnsLastsToDocument")
    default Integer mapTurnsLastsToDocument(Effect effect) {
        if (effect instanceof Expirable) {
            return ((Expirable) effect).getTurnsLasts();
        } else {
            return null;
        }
    }

    @Named("mapIsAccumulatedToDocument")
    default Boolean mapIsAccumulatedToDocument(Effect effect) {
        if (effect instanceof Expirable) {
            return ((Expirable) effect).getIsAccumulated();
        } else {
            return null;
        }
    }

    @Named("mapHasFirstTurnPassedToDocument")
    default Boolean mapHasFirstTurnPassedToDocument(Effect effect) {
        if (effect instanceof Expirable) {
            return ((Expirable) effect).getHasFirstTurnPassed();
        } else {
            return null;
        }
    }

    default MonsterEffectAttribute mapToMonsterAttribute(EffectAttribute effectAttribute) {
        if (effectAttribute instanceof MonsterEffectAttribute) {
            return (MonsterEffectAttribute) effectAttribute;
        } else {
            return null;
        }
    }

    default PlayerEffectAttribute mapToPlayerAttribute(EffectAttribute effectAttribute) {
        if (effectAttribute instanceof PlayerEffectAttribute) {
            return (PlayerEffectAttribute) effectAttribute;
        } else {
            return null;
        }
    }

}
