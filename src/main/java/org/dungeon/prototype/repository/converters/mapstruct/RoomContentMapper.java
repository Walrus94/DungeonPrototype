package org.dungeon.prototype.repository.converters.mapstruct;

import lombok.val;
import org.dungeon.prototype.model.document.room.RoomContentDocument;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.room.content.BonusRoom;
import org.dungeon.prototype.model.room.content.EmptyRoom;
import org.dungeon.prototype.model.room.content.HealthShrine;
import org.dungeon.prototype.model.room.content.ManaShrine;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.model.room.content.NoContentRoom;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.model.room.content.Shrine;
import org.dungeon.prototype.model.room.content.Treasure;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Mapper(uses = {MonsterMapper.class, ItemMapper.class})
public interface RoomContentMapper {
    RoomContentMapper INSTANCE = Mappers.getMapper(RoomContentMapper.class);

    @Mappings({
            @Mapping(target = "monster", ignore = true),
            @Mapping(target = "gold", ignore = true),
            @Mapping(target = "items", ignore = true)
    })
    RoomContentDocument mapToDocument(NoContentRoom roomContent);

    @Mapping(target = "monster", ignore = true)
    RoomContentDocument mapToDocument(BonusRoom roomContent);

    @Mappings({
            @Mapping(target = "gold", ignore = true),
            @Mapping(target = "items", ignore = true)
    })
    RoomContentDocument mapToDocument(MonsterRoom roomContent);

    @Mappings({
            @Mapping(target = "monster", ignore = true),
            @Mapping(target = "gold", ignore = true),
            @Mapping(target = "items", ignore = true)
    })
    RoomContentDocument mapToDocument(Shrine roomContent);

    default RoomContent mapToRoomContent(RoomContentDocument document) {
        if (isNull(document)) {
            return null;
        }
        return switch (document.getRoomType()) {
            case WEREWOLF, SWAMP_BEAST, DRAGON, ZOMBIE, VAMPIRE -> {
                val room = new MonsterRoom();
                room.setMonster(MonsterMapper.INSTANCE.mapToMonster(document.getMonster()));
                yield room;
            }
            case START, END, NORMAL,
                    DRAGON_KILLED, WEREWOLF_KILLED, SWAMP_BEAST_KILLED, ZOMBIE_KILLED, VAMPIRE_KILLED,
                    SHRINE_DRAINED, TREASURE_LOOTED -> new EmptyRoom(document.getRoomType());

            case MERCHANT -> {
                val room = new Merchant();
                room.setItems(convertItems(document));
                yield room;
            }
            case TREASURE -> {
                val room = new Treasure();
                room.setGold(isNull(document.getGold()) ? 0 : document.getGold());
                room.setItems(convertItems(document));
                yield room;
            }

            case MANA_SHRINE -> new ManaShrine();
            case HEALTH_SHRINE -> new HealthShrine();
        };
    }

    default RoomContentDocument mapToRoomContentDocument(RoomContent roomContent) {
        if (isNull(roomContent)) {
            return null;
        }
        return switch (roomContent.getRoomType()) {
            case WEREWOLF, SWAMP_BEAST, DRAGON, ZOMBIE, VAMPIRE -> RoomContentMapper.INSTANCE.mapToDocument((MonsterRoom) roomContent);
            case START, END, NORMAL,
                    DRAGON_KILLED, WEREWOLF_KILLED, SWAMP_BEAST_KILLED, ZOMBIE_KILLED, VAMPIRE_KILLED,
                    SHRINE_DRAINED, TREASURE_LOOTED -> RoomContentMapper.INSTANCE.mapToDocument((NoContentRoom) roomContent);
            case MERCHANT, TREASURE -> RoomContentMapper.INSTANCE.mapToDocument((BonusRoom) roomContent);
            case HEALTH_SHRINE, MANA_SHRINE -> RoomContentMapper.INSTANCE.mapToDocument((Shrine) roomContent);
        };
    }

    @NotNull
    private static Set<Item> convertItems(RoomContentDocument document) {
        return document.getItems().stream().map(itemDocument ->
                switch (itemDocument.getItemType()) {
                    case WEARABLE -> ItemMapper.INSTANCE.mapToWearable(itemDocument);
                    case WEAPON -> ItemMapper.INSTANCE.mapToWeapon(itemDocument);
                }
        ).collect(Collectors.toSet());
    }
}
