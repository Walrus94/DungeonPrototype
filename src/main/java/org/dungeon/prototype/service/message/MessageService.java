package org.dungeon.prototype.service.message;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.ChatStateUpdate;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.ExpirableEffect;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.properties.MessagingConstants;
import org.dungeon.prototype.service.PlayerLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.math3.util.FastMath.max;
import static org.dungeon.prototype.bot.ChatState.ACTIVE;
import static org.dungeon.prototype.bot.ChatState.AWAITING_NICKNAME;
import static org.dungeon.prototype.properties.CallbackType.ITEM_INVENTORY;
import static org.dungeon.prototype.properties.CallbackType.ITEM_INVENTORY_EQUIP;
import static org.dungeon.prototype.properties.CallbackType.ITEM_INVENTORY_UN_EQUIP;
import static org.dungeon.prototype.properties.CallbackType.MAP;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_BUY_MENU;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_DISPLAY_ITEM;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_PRICE;
import static org.dungeon.prototype.properties.CallbackType.PLAYER_STATS;
import static org.dungeon.prototype.properties.Emoji.AXE;
import static org.dungeon.prototype.properties.Emoji.BLACK_HEART;
import static org.dungeon.prototype.properties.Emoji.BLUE_SQUARE;
import static org.dungeon.prototype.properties.Emoji.BROWN_BLOCK;
import static org.dungeon.prototype.properties.Emoji.DAGGER;
import static org.dungeon.prototype.properties.Emoji.DIAMOND;
import static org.dungeon.prototype.properties.Emoji.HEART;
import static org.dungeon.prototype.properties.Emoji.ORANGE_BLOCK;
import static org.dungeon.prototype.properties.Emoji.RED_SQUARE;
import static org.dungeon.prototype.properties.Emoji.SHIELD;
import static org.dungeon.prototype.properties.Emoji.STONKS;
import static org.dungeon.prototype.properties.Emoji.SWORD;
import static org.dungeon.prototype.properties.Emoji.TREASURE;
import static org.dungeon.prototype.util.FileUtil.getImage;

@Slf4j
@Service
public class MessageService {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    @Value("${messaging.bar-blocks}")
    private Integer barBlocks;
    @Value("${messaging.xp-bar-blocks}")
    private Integer xpBarBlocks;
    @Value("${messaging.stat-bar-blocks}")
    private Integer statBarBlocks;
    @Autowired
    private MessageSender messageSender;
    @Autowired
    private MessagingConstants messagingConstants;
    @Autowired
    private KeyboardService keyboardService;

    public void sendStartMessage(Long chatId, String nickname) {
        messageSender.sendMessage(
                chatId,
                String.format("Welcome to dungeon, %s!", nickname),
                keyboardService.getStartInlineKeyboardMarkup(false));
    }

    @ChatStateUpdate(from = ACTIVE, to = AWAITING_NICKNAME)
    public void sendRegisterMessage(Long chatId, String suggestedPrompt) {
        messageSender.sendPromptMessage(
                chatId,
                keyboardService.getPromptReplyKeyboardMarkup(suggestedPrompt),
        "Welcome to dungeon!\nPlease, enter nickname to register");
    }

    public void sendContinueMessage(Long chatId, String nickname, Boolean hasSavedGame) {
        messageSender.sendMessage(
                chatId,
                String.format("Welcome to dungeon, %s!", nickname),
                keyboardService.getStartInlineKeyboardMarkup(hasSavedGame));
    }

    public boolean sendRoomMessage(Long chatId, Player player, Room room) {
        String caption;
        if (room.getRoomContent() instanceof MonsterRoom monsterRoom) {
            caption = getRoomMessageCaption(player, monsterRoom.getMonster());
        } else {
            caption = getRoomMessageCaption(player);
        }
        val keyboardMarkup = keyboardService.getRoomInlineKeyboardMarkup(room, player);
        val inputFile = getImage(room);
        if (isNull(inputFile)) {
            return false;
        }
        messageSender.sendPhotoMessage(chatId, caption, keyboardMarkup, inputFile);
        return true;
    }

    public void sendPlayerStatsMessage(Long chatId, Player player) {
        messageSender.sendMessage(
                chatId,
                getPlayerStatsMessageCaption(player),
                keyboardService.getPlayerStatsReplyMarkup());
    }

    public void sendInventoryMessage(Long chatId, Inventory inventory) {
        messageSender.sendMessage(
                chatId,
                "Inventory: ",
                keyboardService.getInventoryReplyMarkup(inventory, ITEM_INVENTORY, ITEM_INVENTORY_UN_EQUIP, ITEM_INVENTORY_EQUIP, List.of(MAP, PLAYER_STATS)));
    }

    public void sendTreasureMessage(Long chatId, Treasure treasure) {
        messageSender.sendMessage(
                chatId,
                "Treasure:",
                keyboardService.getTreasureContentReplyMarkup(treasure));
    }

    public void sendInventoryItemMessage(Long chatId, Item item, CallbackType inventoryType, Optional<String> itemType) {
        if (itemType.isPresent()) {
            messageSender.sendMessage(
                    chatId,
                    getInventoryEquippedItemInfoMessageCaption(item, itemType.get()),
                    keyboardService.getEquippedItemInfoReplyMarkup(inventoryType, item.getSellingPrice()));
        } else {
            messageSender.sendMessage(
                    chatId,
                    getInventoryUnEquippedItemInfoMessageCaption(item),
                    keyboardService.getInventoryItemInfoReplyMarkup(item, inventoryType));
        }
    }

    public void sendMerchantBuyMenuMessage(Long chatId, Integer gold, Set<Item> items) {
        messageSender.sendMessage(
                chatId,
                "Gold: " + gold + "\nAvailable items:",
                keyboardService.getMerchantBuyListReplyMarkup(items));
    }

    public void sendMerchantBuyItemMessage(Long chatId, Item item) {
        messageSender.sendMessage(
                chatId,
                getMerchantSellItemInfoMessageCaption(item),
                keyboardService.getMerchantBuyItemInfoReplyMarkup(item));
    }

    public void sendMerchantSellMenuMessage(Long chatId, Player player) {
        messageSender.sendMessage(
                chatId,
                "Gold: " + player.getGold() + "\nSell your items:",
                keyboardService.getInventoryReplyMarkup(player.getInventory(),
                        MERCHANT_SELL_DISPLAY_ITEM,
                        MERCHANT_SELL_PRICE,
                        MERCHANT_SELL_PRICE,
                        Collections.singletonList(MERCHANT_BUY_MENU)));
    }

    public void sendLevelUpgradeMessage(Long chatId, Player player) {
        messageSender.sendMessage(
                chatId,
                "New level reached! Choose upgrade:",
                keyboardService.getNewLevelUpgradeReplyMarkup(player));
    }

    public void sendDeathMessage(Long chatId) {
        messageSender.sendMessage(
                chatId,
                "You are dead!",
                keyboardService.getDeathMessageInlineKeyboardMarkup());
    }

    private String getRoomMessageCaption(Player player, Monster monster) {
        val emoji = messagingConstants.getEmoji();
        return emoji.get(HEART) + generateBar(player.getHp(), player.getMaxHp(), emoji.get(RED_SQUARE), barBlocks) +
                " -> " + emoji.get(BLACK_HEART) + " " + generateBar(monster.getHp(), monster.getMaxHp(), emoji.get(RED_SQUARE), barBlocks) + "\n" +
                emoji.get(DIAMOND) + ": " + generateBar(player.getMana(), player.getMaxMana(), emoji.get(BLUE_SQUARE), barBlocks) +
                //TODO: consider moving to attack buttons
                " -> " + emoji.get(AXE) + monster.getPrimaryAttack().getAttack() + "\n" + //TODO: use current attack after callback handling improvement
                emoji.get(SHIELD) + ": " + player.getDefense() + "\n" +
                "Gold: " + player.getGold() + " " + emoji.get(TREASURE) + "\n" +
                emoji.get(STONKS) + ": " + generateXpBar(player.getXp(), player.getPlayerLevel(), player.getNextLevelXp());
    }

    private String getRoomMessageCaption(Player player) {
        val emoji = messagingConstants.getEmoji();
        return emoji.get(HEART) + " " + generateBar(player.getHp(), player.getMaxHp(), emoji.get(RED_SQUARE), barBlocks) + "\n" +
                emoji.get(DIAMOND) + " " + generateBar(player.getMana(), player.getMaxMana(), emoji.get(BLUE_SQUARE), barBlocks) + "\n" +
                emoji.get(SHIELD) + " " + player.getDefense() + "\n" +
                "Gold: " + player.getGold() + " " + emoji.get(TREASURE) + "\n" +
                emoji.get(STONKS) + ": " + generateXpBar(player.getXp(), player.getPlayerLevel(), player.getNextLevelXp());
    }

    private String getInventoryUnEquippedItemInfoMessageCaption(Item item) {
        val itemDescription = getInventoryItemDescription(item);
        val effectsDescription = getEffectsDescription(item.getEffects());
        return itemDescription + effectsDescription;
    }

    private static String getInventoryEquippedItemInfoMessageCaption(Item item, String itemType) {
        val itemDescription = getInventoryItemDescription(item);
        val effectsDescription = getEffectsDescription(item.getEffects());
        return "*" + itemType + ":* " + itemDescription + effectsDescription;
    }

    private static String getMerchantSellItemInfoMessageCaption(Item item) {
        val itemDescription = getInventoryItemDescription(item);
        val effectsDescription = getEffectsDescription(item.getEffects());
        return itemDescription + effectsDescription;
    }

    private static <T extends Effect> String getEffectsDescription(List<T> effects) {
        return "Effects: \n" + effects.stream().map(Effect::toString).collect(Collectors.joining("\n"));
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
                val isCompleteDragonBone = weapon.getIsCompleteDragonBone();

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

    public void sendMapMenuMessage(Long chatId, String levelMap) {
       messageSender.sendMessage(
                chatId,
                levelMap,
                keyboardService.getMapInlineKeyboardMarkup());
    }

    private String getPlayerStatsMessageCaption(Player player) {
        val emoji = messagingConstants.getEmoji();
        val permanentEffects = player.getEffects().stream()
                .filter(Effect::isPermanent).toList();
        val expirableEffects = player.getEffects().stream()
                .filter(effect -> !effect.isPermanent()).toList();
        return "*Player's stats:* \n" +
                emoji.get(HEART) + " " + generateBar(player.getHp(), player.getMaxHp(), emoji.get(RED_SQUARE), statBarBlocks) + "\n" +
                emoji.get(DIAMOND) + " " + generateBar(player.getMana(), player.getMaxMana(), emoji.get(BLUE_SQUARE), statBarBlocks) + "\n" +
                "_Basic Attributes:_ " +
                player.getAttributes().entrySet().stream()
                        .map(entry -> "- " + entry.getKey().toString() + ": " + entry.getValue())
                        .collect(Collectors.joining("\n", "\n", "\n")) +
                emoji.get(SHIELD) + ": " + player.getDefense() + " / " + player.getMaxDefense() + "\n" +
                emoji.get(SWORD) + ": " + player.getPrimaryAttack().getAttack() + "\n" +
                (nonNull(player.getSecondaryAttack()) && player.getSecondaryAttack().getAttack() > 0 ? emoji.get(DAGGER) + player.getSecondaryAttack().getAttack() + "\n" : "")  +
                (!permanentEffects.isEmpty() ? "_Item effects:_ " +
                        permanentEffects.stream()
                                .map(Effect::toString)
                                .collect(Collectors.joining("\n", "\n", "\n")) : "") +
                (!expirableEffects.isEmpty() ? "_Expirable effects:_ " +
                        player.getEffects().stream()
                                .filter(effect -> !effect.isPermanent())
                                .map(effect -> effect + ", expires in " + ((ExpirableEffect) effect).getTurnsLasts() + " turns")
                                .collect(Collectors.joining("\n", "\n", "\n")) : "") +
                emoji.get(STONKS) + ": " + player.getXp() + " xp " + generateXpBar(player.getXp(), player.getPlayerLevel(), player.getNextLevelXp());
    }

    private String generateBar(Integer current, Integer max, String emoji, Integer barBlocks) {
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
