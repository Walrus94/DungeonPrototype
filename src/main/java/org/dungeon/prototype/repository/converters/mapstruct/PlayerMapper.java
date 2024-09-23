package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.item.EffectDocument;
import org.dungeon.prototype.model.document.player.PlayerDocument;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.player.Player;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Mapper(uses = {
        InventoryMapper.class,
        EffectMapper.class,
        PlayerAttackMapper.class
})
public interface PlayerMapper {
    PlayerMapper INSTANCE = Mappers.getMapper(PlayerMapper.class);

    @Mapping(target = "effects", source = "effects", qualifiedByName = "mapEffectsDocuments")
    PlayerDocument mapToDocument(Player player);

    @Mappings({
            @Mapping(target = "effects", source = "effects", qualifiedByName = "mapEffects"),
    })
    Player mapToPlayer(PlayerDocument document);

    @Named("mapEffects")
    default List<Effect> mapEffects(List<EffectDocument> documents) {
        if (isNull(documents)) {
            return new ArrayList<>();
        }

        return documents.stream().map(EffectMapper.INSTANCE::mapToEffect).collect(Collectors.toList());
    }

    @Named("mapEffectsDocuments")
    default List<EffectDocument> mapEffectsDocuments(List<Effect> effects) {
        if (isNull(effects)) {
            return new ArrayList<>();
        }
        return effects.stream().map(EffectMapper.INSTANCE::mapToDocument).collect(Collectors.toList());
    }
}
