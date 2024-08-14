package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.item.EffectDocument;
import org.dungeon.prototype.model.document.player.PlayerDocument;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.PlayerEffect;
import org.dungeon.prototype.model.effect.attributes.PlayerEffectAttribute;
import org.dungeon.prototype.model.player.Player;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Mapper(uses = {
        InventoryMapper.class,
        EffectMapper.class
})
public interface PlayerMapper {
    PlayerMapper INSTANCE = Mappers.getMapper(PlayerMapper.class);

    @Mapping(target = "effects", source = "effects", qualifiedByName = "mapEffectsDocuments")
    PlayerDocument mapToDocument(Player player);

    @Mapping(target = "effects", source = "effects", qualifiedByName = "mapEffects")
    Player mapToPlayer(PlayerDocument document);

    @Named("mapEffects")
    default Map<PlayerEffectAttribute, PriorityQueue<PlayerEffect>> mapEffects(Map<PlayerEffectAttribute, List<EffectDocument>> documents) {
        if (isNull(documents)) {
            return null;
        }

        return documents.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue()
                        .stream()
                        .map(EffectMapper.INSTANCE::mapToPlayerEffect)
                        .collect(Collectors.toCollection(() ->
                                new PriorityQueue<>(Comparator.comparing(Effect::getAction))))));
    }

    @Named("mapEffectsDocuments")
    default Map<PlayerEffectAttribute, List<EffectDocument>> mapEffectsDocuments(Map<PlayerEffectAttribute, PriorityQueue<PlayerEffect>> effects) {
        if (isNull(effects)) {
            return null;
        }

        return effects.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue()
                        .stream()
                        .map(EffectMapper.INSTANCE::mapToDocument)
                        .collect(Collectors.toList())));
    }
}
