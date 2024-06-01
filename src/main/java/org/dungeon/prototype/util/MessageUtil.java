package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.content.Monster;

@UtilityClass
public class MessageUtil {
    public static String getRoomMessageCaption(Player player, Monster monster) {
        return "\uD83D\uDC9F: " + player.getHp() + " / " + player.getMaxHp() +
                " -> \uD83D\uDDA4 " + monster.getHp() + " / " + monster.getMaxHp() + "\n" +
                "\uD83D\uDC8E: " + player.getMana() + " / " + player.getMaxMana() + "\n" +
                "\uD83D\uDC4A: " + player.getAttack() +
                " -> \uD83E\uDE93" + monster.getAttack() + "\n" +
                "\uD83D\uDEE1: " + player.getDefense() + "\n" +
                "\uD83D\uDCC8: " + player.getXp() + "\n" +
                "\uD83E\uDD11: " + player.getGold();
    }

    public static String getRoomMessageCaption(Player player) {
        return "\uD83D\uDC9F: " + player.getHp() + " / " + player.getMaxHp() + "\n" +
                "\uD83D\uDC8E: " + player.getMana() + " / " + player.getMaxMana() + "\n" +
                "\uD83D\uDC4A: " + player.getAttack() + "\n" +
                "\uD83D\uDEE1: " + player.getDefense() + "\n" +
                "\uD83D\uDCC8: " + player.getXp() + "\n" +
                "\uD83E\uDD11: " + player.getGold();
    }
}
