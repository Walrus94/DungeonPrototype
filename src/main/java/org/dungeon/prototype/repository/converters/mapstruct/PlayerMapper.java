package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.item.EffectDocument;
import org.dungeon.prototype.model.document.player.PlayerDocument;
import org.dungeon.prototype.model.effect.PlayerEffect;
import org.dungeon.prototype.model.player.Player;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
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
    default List<PlayerEffect> mapEffects(List<EffectDocument> documents) {
        if (isNull(documents)) {
            return null;
        }

        return documents.stream().map(EffectMapper.INSTANCE::mapToPlayerEffect).collect(Collectors.toList());
    }

    @Named("mapEffectsDocuments")
    default List<EffectDocument> mapEffectsDocuments(List<PlayerEffect> effects) {
        if (isNull(effects)) {
            return null;
        }
        return effects.stream().map(EffectMapper.INSTANCE::mapToDocument).collect(Collectors.toList());
    }
}
