package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.service.PlayerLevelService;

import java.util.Objects;

import static org.apache.commons.math3.util.FastMath.max;

@UtilityClass
public class MessageUtil {

    private static final Integer BAR_BLOCKS = 5;
    private static final Integer XP_BAR_BLOCKS = 10;
    public static String getRoomMessageCaption(Player player, Monster monster) {
        return "\uD83D\uDC9F: " + generateBar(player.getHp(), player.getMaxHp(), "\uD83D\uDFE5") + "\n" +
                " -> \uD83D\uDDA4 " + generateBar(monster.getHp(), monster.getMaxHp(), "\uD83D\uDFE5")+ "\n" +
                "\uD83D\uDC8E: " + generateBar(player.getMana(), player.getMaxMana(), "\uD83D\uDFE6") + "\n" +
                (Objects.nonNull(player.getInventory().getWeaponSet().getPrimaryWeapon()) ? "\uD83D\uDDE1: " + player.getInventory().getWeaponSet().getPrimaryWeapon().getAttack(): "") +
                " -> \uD83E\uDE93" + monster.getPrimaryAttack().getAttack() + "\n" +
                (Objects.nonNull(player.getInventory().getWeaponSet().getSecondaryWeapon()) ? "\uD83D\uDD2A" + player.getInventory().getWeaponSet().getSecondaryWeapon().getAttack() + "\n" : "") +
                " -> \uD83E\uDE93" + monster.getSecondaryAttack().getAttack() + "\n" +
                "\uD83D\uDEE1: " + player.getDefense() + "\n" +
                "\uD83E\uDD11: " + player.getGold() + "\n" +
                "\uD83D\uDCC8: " + generateXpBar(player.getXp(), player.getPlayerLevel(), player.getNextLevelXp());
    }

    public static String getRoomMessageCaption(Player player) {
        return "\uD83D\uDC9F: " + generateBar(player.getHp(), player.getMaxHp(), "\uD83D\uDFE5") + "\n" +
                "\uD83D\uDC8E: " + generateBar(player.getMana(), player.getMaxMana(), "\uD83D\uDFE6") + "\n" +
                (Objects.nonNull(player.getInventory().getWeaponSet().getPrimaryWeapon()) ? "\uD83D\uDDE1: " + player.getInventory().getWeaponSet().getPrimaryWeapon().getAttack() + "\n" : "") +
                (Objects.nonNull(player.getInventory().getWeaponSet().getSecondaryWeapon()) ? "\uD83D\uDD2A" + player.getInventory().getWeaponSet().getSecondaryWeapon().getAttack() + "\n" : "") +
                "\uD83D\uDEE1: " + player.getDefense() + "\n" +
                "\uD83E\uDD11: " + player.getGold() + "\n" +
                "\uD83D\uDCC8: " + generateXpBar(player.getXp(), player.getPlayerLevel(), player.getNextLevelXp());
    }

    private static String generateBar(Integer current, Integer max, String emoji) {
        int filledBlocks = (int) ((double) current / max * BAR_BLOCKS);
        int emptyBlocks = BAR_BLOCKS - filledBlocks;

        return emoji.repeat(filledBlocks) +
                "\uD83D\uDFEB".repeat(emptyBlocks) +
                " " +
                current +
                "/" +
                max;
    }

    private static String generateXpBar(Long currentXp, Integer currentLevel, Long nextLevelXp) {
        Long currentLevelXp = PlayerLevelService.calculateXPForLevel(currentLevel);

        int xpInCurrentLevel = (int) (currentXp - currentLevelXp);
        int xpForNextLevel = (int) (nextLevelXp - currentLevelXp);

        double progress = (double) xpInCurrentLevel / xpForNextLevel;
        int filledLength = (int) (XP_BAR_BLOCKS * progress);
        int emptyLength = XP_BAR_BLOCKS - filledLength;

        return "Lvl " + currentLevel +
                "\uD83D\uDFE7".repeat(max(0, filledLength)) +
                "\uD83D\uDFEB".repeat(max(0, emptyLength)) +
                xpForNextLevel + " XP";
    }
}
