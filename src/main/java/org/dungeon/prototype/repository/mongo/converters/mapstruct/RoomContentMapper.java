package org.dungeon.prototype.repository.mongo.converters.mapstruct;

import jakarta.validation.constraints.NotNull;
import lombok.val;
import org.dungeon.prototype.model.document.room.RoomContentDocument;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.room.content.*;
import org.dungeon.prototype.model.room.content.ItemsRoom;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Mapper(uses = {MonsterMapper.class, EffectMapper.class, ItemMapper.class, WeightMapper.class})
public interface RoomContentMapper {
    RoomContentMapper INSTANCE = Mappers.getMapper(RoomContentMapper.class);

    @Mappings({
            @Mapping(target = "monster", ignore = true),
            @Mapping(target = "gold", ignore = true),
            @Mapping(target = "items", ignore = true),
            @Mapping(target = "chanceToBreakWeapon", ignore = true),
            @Mapping(target = "attackBonus", ignore = true),
            @Mapping(target = "armorRestored", ignore = true),
            @Mapping(target = "effect", ignore = true)
    })
    RoomContentDocument mapToDocument(NoContentRoom roomContent);

    @Mappings({
            @Mapping(target = "monster", ignore = true),
            @Mapping(target = "chanceToBreakWeapon", ignore = true),
            @Mapping(target = "attackBonus", ignore = true),
            @Mapping(target = "armorRestored", ignore = true),
            @Mapping(target = "effect", ignore = true),
            @Mapping(target = "gold", source = "roomContent", qualifiedByName = "mapGoldToDocument")
    })
    RoomContentDocument mapToDocument(ItemsRoom roomContent);

    @Mappings({
            @Mapping(target = "gold", ignore = true),
            @Mapping(target = "items", ignore = true),
            @Mapping(target = "chanceToBreakWeapon", ignore = true),
            @Mapping(target = "attackBonus", ignore = true),
            @Mapping(target = "armorRestored", ignore = true),
            @Mapping(target = "effect", ignore = true)
    })
    RoomContentDocument mapToDocument(MonsterRoom roomContent);

    @Mappings({
            @Mapping(target = "monster", ignore = true),
            @Mapping(target = "gold", ignore = true),
            @Mapping(target = "items", ignore = true),
            @Mapping(target = "chanceToBreakWeapon", ignore = true),
            @Mapping(target = "attackBonus", ignore = true),
            @Mapping(target = "armorRestored", ignore = true),
    })
    RoomContentDocument mapToDocument(Shrine roomContent);

    @Mappings({
            @Mapping(target = "monster", ignore = true),
            @Mapping(target = "gold", ignore = true),
            @Mapping(target = "items", ignore = true),
            @Mapping(target = "effect", ignore = true),
    })
    RoomContentDocument mapToDocument(Anvil anvil);

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
            case ANVIL -> Anvil.builder()
                    .attackBonus(document.getAttackBonus())
                    .chanceToBreakWeapon(document.getChanceToBreakWeapon())
                    .armorRestored(document.isArmorRestored())
                    .build();
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

            case MANA_SHRINE -> {
                val room = new ManaShrine();
                room.setEffect(EffectMapper.INSTANCE.mapToExpirableAdditionEffect(document.getEffect()));
                yield room;
            }
            case HEALTH_SHRINE -> {
                val room = new HealthShrine();
                room.setEffect(EffectMapper.INSTANCE.mapToExpirableAdditionEffect(document.getEffect()));
                yield room;
            }
        };
    }

    @Named("mapGoldToDocument")
    default Integer mapGoldToDocument(ItemsRoom roomContent) {
        if (roomContent instanceof Treasure) {
            return ((Treasure) roomContent).getGold();
        } else {
            return null;
        }
    }

    default RoomContentDocument mapToRoomContentDocument(RoomContent roomContent) {
        if (isNull(roomContent)) {
            return null;
        }
        return switch (roomContent.getRoomType()) {
            case WEREWOLF, SWAMP_BEAST, DRAGON, ZOMBIE, VAMPIRE -> RoomContentMapper.INSTANCE.mapToDocument((MonsterRoom) roomContent);
            case ANVIL -> RoomContentMapper.INSTANCE.mapToDocument((Anvil) roomContent);
            case START, END, NORMAL,
                    DRAGON_KILLED, WEREWOLF_KILLED, SWAMP_BEAST_KILLED, ZOMBIE_KILLED, VAMPIRE_KILLED,
                    SHRINE_DRAINED, TREASURE_LOOTED -> RoomContentMapper.INSTANCE.mapToDocument((NoContentRoom) roomContent);
            case MERCHANT, TREASURE -> RoomContentMapper.INSTANCE.mapToDocument((ItemsRoom) roomContent);
            case HEALTH_SHRINE -> RoomContentMapper.INSTANCE.mapToDocument((HealthShrine) roomContent);
            case MANA_SHRINE -> RoomContentMapper.INSTANCE.mapToDocument((ManaShrine) roomContent);
        };
    }

    @NotNull
    private static Set<Item> convertItems(RoomContentDocument document) {
        return document.getItems().stream().map(itemDocument ->
                switch (itemDocument.getItemType()) {
                    case WEARABLE -> ItemMapper.INSTANCE.mapToWearable(itemDocument);
                    case WEAPON -> ItemMapper.INSTANCE.mapToWeapon(itemDocument);
                    case USABLE -> ItemMapper.INSTANCE.mapToUsable(itemDocument);
                }
        ).collect(Collectors.toSet());
    }
}
