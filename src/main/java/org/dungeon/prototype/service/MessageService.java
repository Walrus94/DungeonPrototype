package org.dungeon.prototype.service;

import lombok.val;
import org.dungeon.prototype.model.effect.ItemEffect;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.properties.MessagingConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.apache.commons.math3.util.FastMath.max;
import static org.dungeon.prototype.properties.Emoji.*;

@Service
public class MessageService {

    @Value("${messaging.bar-blocks}")
    private Integer barBlocks;
    @Value("${messaging.xp-bar-blocks}")
    private Integer xpBarBlocks;
    @Autowired
    private MessagingConstants messagingConstants;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    public String getRoomMessageCaption(Player player, Monster monster) {
        val emoji = messagingConstants.getEmoji();
        return emoji.get(HEART) + " : " + generateBar(player.getHp(), player.getMaxHp(), emoji.get(RED_SQUARE)) + "\n" +
                " -> " + emoji.get(BLACK_HEART) + " : " + generateBar(monster.getHp(), monster.getMaxHp(), emoji.get(RED_SQUARE))+ "\n" +
                emoji.get(DIAMOND) + ": " + generateBar(player.getMana(), player.getMaxMana(), emoji.get(BLUE_SQUARE)) + "\n" +
                (nonNull(player.getInventory().getWeaponSet().getPrimaryWeapon()) ? emoji.get(SWORD) + ": " + player.getInventory().getWeaponSet().getPrimaryWeapon().getAttack(): "") +
                " -> " + emoji.get(AXE) + monster.getPrimaryAttack().getAttack() + "\n" +
                (nonNull(player.getInventory().getWeaponSet().getSecondaryWeapon()) ? emoji.get(DAGGER) + player.getInventory().getWeaponSet().getSecondaryWeapon().getAttack() + "\n" : "") +
                " -> " + emoji.get(AXE) + monster.getSecondaryAttack().getAttack() + "\n" +
                emoji.get(SHIELD) + ": " + player.getDefense() + "\n" +
                "Gold: " + player.getGold() + " " + emoji.get(TREASURE) + "\n" +
                emoji.get(STONKS) + ": " + generateXpBar(player.getXp(), player.getPlayerLevel(), player.getNextLevelXp());
    }

    public String getRoomMessageCaption(Player player) {
        val emoji = messagingConstants.getEmoji();
        return emoji.get(HEART) + ": " + generateBar(player.getHp(), player.getMaxHp(), emoji.get(RED_SQUARE)) + "\n" +
                emoji.get(DIAMOND) + ": " + generateBar(player.getMana(), player.getMaxMana(), emoji.get(BLUE_SQUARE)) + "\n" +
                (nonNull(player.getInventory().getWeaponSet().getPrimaryWeapon()) ? emoji.get(SWORD) + ": " + player.getInventory().getWeaponSet().getPrimaryWeapon().getAttack() + "\n" : "") +
                (nonNull(player.getInventory().getWeaponSet().getSecondaryWeapon()) ? emoji.get(DAGGER) + player.getInventory().getWeaponSet().getSecondaryWeapon().getAttack() + "\n" : "") +
                emoji.get(SHIELD)+ ": " + player.getDefense() + "\n" +
                "Gold: " + player.getGold() + " " + emoji.get(TREASURE) + "\n" +
                emoji.get(STONKS) + ": " + generateXpBar(player.getXp(), player.getPlayerLevel(), player.getNextLevelXp());
    }

    public String getInventoryUnEquippedItemInfoMessageCaption(Item item) {
        val itemDescription = getInventoryItemDescription(item);
        val effectsDescription = getEffectsDescription(item.getEffects());
        return itemDescription + effectsDescription;
    }

    public String getInventoryEquippedItemInfoMessageCaption(Item item, String itemType) {
        val itemDescription = getInventoryItemDescription(item);
        val effectsDescription = getEffectsDescription(item.getEffects());
        return "*" + itemType + ":* " + itemDescription + effectsDescription;
    }

    public String getMerchantSellItemInfoMessageCaption(Item item) {
        val itemDescription = getInventoryItemDescription(item);
        val effectsDescription = getEffectsDescription(item.getEffects());
        return itemDescription + effectsDescription;
    }

    private static String getEffectsDescription(List<ItemEffect> effects) {
        return "Effects: \n" + effects.stream().map(ItemEffect::toString).collect(Collectors.joining("\n"));
    }

    //TODO: get rid of it
    public String formatItemType(CallbackType equippedType) {
        return switch (equippedType) {
            case VEST -> "Vest";
            case GLOVES -> "Gloves";
            case BOOTS -> "Boots";
            case HEAD -> "Head";
            case LEFT_HAND -> "Secondary weapon";
            case RIGHT_HAND -> "Primary weapon";
            default -> "Item";
        };
    }

    @NotNull
    private static String getInventoryItemDescription(Item item) {
        return switch (item.getItemType()) {
            case WEAPON -> {
                val weapon = (Weapon) item;
                val description = weapon.getAttributes().toString();
                val attack = weapon.getAttack();
                val chanceToMiss = weapon.getChanceToMiss();
                val chanceToKnockOut = weapon.getChanceToKnockOut();
                val criticalHitChance = weapon.getCriticalHitChance();
                val isCompleteDragonBone = weapon.isCompleteDragonBone();

                yield item.getName() + "\n" + description + "\n" +
                        "attack: " + attack + "\n" + "Chance to miss: " + DECIMAL_FORMAT.format(chanceToMiss) + "\n" +
                        "chance to knock out: " + DECIMAL_FORMAT.format(chanceToKnockOut) + "\n" +
                        "critical hit chance: " + DECIMAL_FORMAT.format(criticalHitChance) + "\n" +
                        (weapon.getHasMagic() ? "Magic type: " + weapon.getMagicType() : "") + "\n" +
                        (isCompleteDragonBone ? "This weapon is made completely of Dragon bone!\n" : "\n");

            }
            case WEARABLE -> {
                val wearable = (Wearable) item;
                val description = wearable.getAttributes().toString();
                val armor = wearable.getArmor();
                val chanceToDodge = wearable.getChanceToDodge();
                yield item.getName() + "\n" + description + "\n" +
                        "armor: " + armor + "\n" +
                        (nonNull(chanceToDodge) && chanceToDodge > 0.0 ? "chance to dodge: " + DECIMAL_FORMAT.format(chanceToDodge) + "\n" : "") +
                        (wearable.getHasMagic() ? "Magic type: " + wearable.getMagicType() : "");
            }
            case USABLE -> "";
        };
    }
    private String generateBar(Integer current, Integer max, String emoji) {
        val emojis = messagingConstants.getEmoji();
        int filledBlocks = (int) ((double) current / max * barBlocks);
        int emptyBlocks = barBlocks - filledBlocks;

        return emoji.repeat(filledBlocks) +
                emojis.get(BROWN_BLOCK).repeat(emptyBlocks) +
                " " +
                current +
                "/" +
                max;
    }


    private String generateXpBar(Long currentXp, Integer currentLevel, Long nextLevelXp) {
        Long currentLevelXp = PlayerLevelService.calculateXPForLevel(currentLevel);
        val emojis = messagingConstants.getEmoji();

        int xpInCurrentLevel = (int) (currentXp - currentLevelXp);
        int xpForNextLevel = (int) (nextLevelXp - currentLevelXp);

        double progress = (double) xpInCurrentLevel / xpForNextLevel;
        int filledLength = (int) (xpBarBlocks * progress);
        int emptyLength = xpBarBlocks - filledLength;

        return "Lvl " + currentLevel +
                messagingConstants.getEmoji().get(ORANGE_BLOCK).repeat(max(0, filledLength)) +
                emojis.get(BROWN_BLOCK).repeat(max(0, emptyLength)) +
                xpForNextLevel + " XP";
    }
}
