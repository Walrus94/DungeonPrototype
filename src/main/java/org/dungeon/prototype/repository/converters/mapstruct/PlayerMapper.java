package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.player.PlayerDocument;
import org.dungeon.prototype.model.player.Player;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = InventoryMapper.class)
public interface PlayerMapper {
    PlayerMapper INSTANCE = Mappers.getMapper(PlayerMapper.class);

    PlayerDocument mapToDocument(Player player);

    Player mapToPlayer(PlayerDocument document);
}
