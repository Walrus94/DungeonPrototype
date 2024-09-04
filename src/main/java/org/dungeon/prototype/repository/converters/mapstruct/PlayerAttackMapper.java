package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.player.PlayerAttackDocument;
import org.dungeon.prototype.model.player.PlayerAttack;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PlayerAttackMapper {
    PlayerAttackMapper INSTANCE = Mappers.getMapper(PlayerAttackMapper.class);

    PlayerAttackDocument mapToDocument(PlayerAttack playerAttack);

    PlayerAttack mapToPlayerAttack(PlayerAttackDocument document);
}
