package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.RoomType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class RoomGenerationUtils {
    public static Integer calculateExpectedRange(Player player) {
        log.debug("Calculating expected weight range...");
        val playerAttributes = player.getAttributes().values().stream().filter(Objects::nonNull).mapToInt(v -> v).sum();
        log.debug("Player attributes sum: {}", playerAttributes);
        log.debug("Player defense: {}", player.getDefense());
        log.debug("Player hp: {}/{}, Player mana: {}/{}", player.getHp(), player.getMaxHp(), player.getMana(), player.getMaxMana());
        if (player.getHp().equals(player.getMaxHp()) || player.getMana().equals(player.getMaxMana())) {
            if (!player.getMana().equals(player.getMaxMana())) {
                return playerAttributes * player.getDefense() / (player.getMaxMana() - player.getMana());
            }
            if (!player.getHp().equals(player.getMaxHp())) {
                return playerAttributes * player.getDefense() / (player.getMaxHp() - player.getHp());
            } else {
                return playerAttributes * player.getDefense();
            }
        }
        return 10 * playerAttributes * player.getDefense() /
                (player.getMaxHp() - player.getHp() * (player.getMaxMana() - player.getMana()));
    }

    public static Integer calculateExpectedWeightAbs(Player player) {
        log.debug("Calculating expected absolute weight...");
        val playerInventoryWeight = Stream.concat(player.getInventory().getItems().stream(), Stream.concat(player.getInventory().getArmorItems().stream(),
                player.getInventory().getWeapons().stream())).filter(Objects::nonNull).mapToInt(Item::getWeight).sum();
        log.debug("Player inventory weight: {}", playerInventoryWeight);
        log.debug("Player gold: {}, player level: {}, next level xp: {}, player xp: {}",
                player.getGold(), player.getPlayerLevel(), player.getNextLevelXp(), player.getXp());
        return 10 * player.getGold() * playerInventoryWeight * player.getPlayerLevel() /
                (int) (player.getNextLevelXp() - player.getXp());
    }

    public static RoomType convertToRoomType(MonsterClass monsterClass, boolean isAlive) {
        return switch (monsterClass) {
            case WEREWOLF -> isAlive ? RoomType.WEREWOLF : RoomType.WEREWOLF_KILLED;
            case SWAMP_BEAST -> isAlive ? RoomType.SWAMP_BEAST : RoomType.SWAMP_BEAST_KILLED;
            case VAMPIRE -> isAlive ? RoomType.VAMPIRE : RoomType.VAMPIRE_KILLED;
            case DRAGON -> isAlive ? RoomType.DRAGON : RoomType.DRAGON_KILLED;
            case ZOMBIE -> isAlive ? RoomType.ZOMBIE : RoomType.ZOMBIE_KILLED;
        };
    }

    public static MonsterClass convertToMonsterClass(RoomType roomType) {
        return switch (roomType) {
            case ZOMBIE -> MonsterClass.ZOMBIE;
            case WEREWOLF -> MonsterClass.WEREWOLF;
            case VAMPIRE -> MonsterClass.VAMPIRE;
            case SWAMP_BEAST -> MonsterClass.SWAMP_BEAST;
            case DRAGON -> MonsterClass.DRAGON;
            default -> throw new IllegalStateException("Unexpected value: " + roomType);
        };
    }

    public static List<RoomType> getMonsterRoomTypes() {
        return List.of(RoomType.WEREWOLF, RoomType.VAMPIRE, RoomType.SWAMP_BEAST, RoomType.ZOMBIE, RoomType.DRAGON);
    }
}
