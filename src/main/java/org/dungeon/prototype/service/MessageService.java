package org.dungeon.prototype.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.MessageSending;
import org.dungeon.prototype.annotations.aspect.PhotoMessageSending;
import org.dungeon.prototype.model.effect.Expirable;
import org.dungeon.prototype.model.effect.ItemEffect;
import org.dungeon.prototype.model.effect.PlayerEffect;
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
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.apache.commons.math3.util.FastMath.max;
import static org.dungeon.prototype.properties.CallbackType.*;
import static org.dungeon.prototype.properties.Emoji.*;
import static org.dungeon.prototype.util.FileUtil.getRoomAsset;
import static org.dungeon.prototype.util.FileUtil.loadImageAsByteArray;

@Slf4j
@Service
public class MessageService {

    @Value("${messaging.bar-blocks}")
    private Integer barBlocks;
    @Value("${messaging.xp-bar-blocks}")
    private Integer xpBarBlocks;
    @Autowired
    private MessagingConstants messagingConstants;
    @Autowired
    private KeyboardService keyboardService;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    @MessageSending
    public SendMessage sendStartMessage(Long chatId, String nickname, Boolean hasSavedGame) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(String.format("Welcome to dungeon, %s!", nickname))
                .replyMarkup(keyboardService.getStartInlineKeyboardMarkup(hasSavedGame))
                .build();
    }

    @MessageSending
    public SendMessage sendMerchantBuyMenuMessage(Long chatId, Integer gold, Set<Item> items) {
        return SendMessage.builder()
                .chatId(chatId)
                .text("Gold: " + gold + "\nAvailable items:")
                .replyMarkup(keyboardService.getMerchantBuyListReplyMarkup(items))
                .build();
    }

    @MessageSending
    public SendMessage sendMerchantBuyItemMessage(Long chatId, Item item) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(getMerchantSellItemInfoMessageCaption(item))
                .replyMarkup(keyboardService.getMerchantBuyItemInfoReplyMarkup(item))
                .build();
    }

    @MessageSending
    public SendMessage sendMerchantSellMenuMessage(Long chatId, Player player) {
        return SendMessage.builder()
                .chatId(chatId)
                .text("Gold: " + player.getGold() + "\nSell your items:")
                .replyMarkup(keyboardService.getInventoryReplyMarkup(player.getInventory(), MERCHANT_SELL_DISPLAY_ITEM, MERCHANT_SELL_PRICE, MERCHANT_SELL_PRICE, Collections.singletonList(MERCHANT_BUY_MENU)))
                .build();
    }

    @MessageSending
    public SendMessage sendMapMenuMessage(Long chatId, String levelMap) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(levelMap)
                .replyMarkup(keyboardService.getMapInlineKeyboardMarkup())
                .build();
    }

    @MessageSending
    public SendMessage sendTreasureMessage(Long chatId, Treasure treasure) {
        return SendMessage.builder()
                .chatId(chatId)
                .text("Treasure:")
                .replyMarkup(keyboardService.getTreasureContentReplyMarkup(treasure))
                .build();
    }

    @MessageSending
    public SendMessage sendLevelUpgradeMessage(Player player, Long chatId) {
        return SendMessage.builder()
                .chatId(chatId)
                .text("New level reached! Choose upgrade:")
                .replyMarkup(keyboardService.getNewLevelUpgradeReplyMarkup(player))
                .build();
    }

    @MessageSending
    public SendMessage sendDeathMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId)
                .text("You are dead!")
                .replyMarkup(keyboardService.getDeathMessageInlineKeyboardMarkup())
                .build();
    }

    @PhotoMessageSending
    public SendPhoto sendRoomMessage(Long chatId, Player player, Room room) {
        String caption;
        if (room.getRoomContent() instanceof MonsterRoom monsterRoom) {
            caption = getRoomMessageCaption(player, monsterRoom.getMonster());
        } else {
            caption = getRoomMessageCaption(player);
        }

        val keyboardMarkup = keyboardService.getRoomInlineKeyboardMarkup(room, player);
        ClassPathResource imgFile = new ClassPathResource(getRoomAsset(room.getRoomContent().getRoomType()));
        try (InputStream inputStream = imgFile.getInputStream()) {
            val imageData = loadImageAsByteArray(inputStream);
            val inputFile = new InputFile();
            inputFile.setMedia(new ByteArrayInputStream(imageData), imgFile.getFilename());
            return SendPhoto.builder()
                    .chatId(chatId)
                    .caption(caption)
                    .photo(inputFile)
                    .replyMarkup(keyboardMarkup)
                    .build();
        } catch (IOException e) {
            log.error("Error loading file: {}", e.getMessage());
            return null;
        }
    }

    @MessageSending
    public SendMessage sendPlayerStatsMessage(Long chatId, Player player) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(getPlayerStatsMessageCaption(player))
                .parseMode("Markdown")
                .replyMarkup(keyboardService.getPlayerStatsReplyMarkup())
                .build();
    }

    @MessageSending
    public SendMessage sendInventoryMessage(Long chatId, Inventory inventory) {
        return SendMessage.builder()
                .chatId(chatId)
                .replyMarkup(keyboardService.getInventoryReplyMarkup(inventory, ITEM_INVENTORY, ITEM_INVENTORY_UN_EQUIP, ITEM_INVENTORY_EQUIP, List.of(MAP, PLAYER_STATS)))
                .text("Inventory")
                .build();
    }

    @MessageSending
    public SendMessage sendItemInfoMessage(Long chatId, Item item, CallbackType inventoryType, Optional<String> itemType) {
        if (itemType.isPresent()) {
            return SendMessage.builder()
                    .chatId(chatId)
                    .text(getInventoryEquippedItemInfoMessageCaption(item, itemType.get()))
                    .parseMode("Markdown")
                    .replyMarkup(keyboardService.getEquippedItemInfoReplyMarkup(inventoryType, item.getSellingPrice()))
                    .build();
        } else {
            return SendMessage.builder()
                    .chatId(chatId)
                    .text(getInventoryUnEquippedItemInfoMessageCaption(item))
                    .parseMode("Markdown")
                    .replyMarkup(keyboardService.getInventoryItemInfoReplyMarkup(item, inventoryType))
                    .build();
        }
    }

    public String getRoomMessageCaption(Player player, Monster monster) {
        val emoji = messagingConstants.getEmoji();
        return emoji.get(HEART) + " : " + generateBar(player.getHp(), player.getMaxHp(), emoji.get(RED_SQUARE)) + "\n" +
                " -> " + emoji.get(BLACK_HEART) + " : " + generateBar(monster.getHp(), monster.getMaxHp(), emoji.get(RED_SQUARE)) + "\n" +
                emoji.get(DIAMOND) + ": " + generateBar(player.getMana(), player.getMaxMana(), emoji.get(BLUE_SQUARE)) + "\n" +
                " -> " + emoji.get(AXE) + monster.getPrimaryAttack().getAttack() + "\n" +
                " -> " + emoji.get(AXE) + monster.getSecondaryAttack().getAttack() + "\n" +
                emoji.get(SHIELD) + ": " + player.getDefense() + "\n" +
                "Gold: " + player.getGold() + " " + emoji.get(TREASURE) + "\n" +
                emoji.get(STONKS) + ": " + generateXpBar(player.getXp(), player.getPlayerLevel(), player.getNextLevelXp());
    }

    public String getRoomMessageCaption(Player player) {
        val emoji = messagingConstants.getEmoji();
        return emoji.get(HEART) + ": " + generateBar(player.getHp(), player.getMaxHp(), emoji.get(RED_SQUARE)) + "\n" +
                emoji.get(DIAMOND) + ": " + generateBar(player.getMana(), player.getMaxMana(), emoji.get(BLUE_SQUARE)) + "\n" +
                emoji.get(SHIELD) + ": " + player.getDefense() + "\n" +
                "Gold: " + player.getGold() + " " + emoji.get(TREASURE) + "\n" +
                emoji.get(STONKS) + ": " + generateXpBar(player.getXp(), player.getPlayerLevel(), player.getNextLevelXp());
    }

    private String getInventoryUnEquippedItemInfoMessageCaption(Item item) {
        val itemDescription = getInventoryItemDescription(item);
        val effectsDescription = getEffectsDescription(item.getEffects());
        return itemDescription + effectsDescription;
    }

    private String getInventoryEquippedItemInfoMessageCaption(Item item, String itemType) {
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

    private String getPlayerStatsMessageCaption(Player player) {
        val emoji = messagingConstants.getEmoji();
        val itemEffects = player.getEffects().stream()
                .filter(effect -> effect instanceof ItemEffect).toList();
        val expirableEffects = player.getEffects().stream()
                .filter(effect -> !effect.isPermanent()).toList();
        return "*Player's stats:* \n" +
                emoji.get(HEART) + ": " + generateBar(player.getHp(), player.getMaxHp(), emoji.get(RED_SQUARE)) + "\n" +
                emoji.get(DIAMOND) + ": " + generateBar(player.getMana(), player.getMaxMana(), emoji.get(BLUE_SQUARE)) + "\n" +
                "_Basic Attributes:_ " +
                player.getAttributes().entrySet().stream()
                        .map(entry -> "- " + entry.getKey().toString() + ": " + entry.getValue())
                        .collect(Collectors.joining("\n", "\n", "\n")) +
                emoji.get(SHIELD) + ": " + player.getDefense() + " / " + player.getMaxDefense() + "\n" +
                emoji.get(SWORD) + ": " + player.getPrimaryAttack() + "\n" +
                (nonNull(player.getSecondaryAttack()) && player.getSecondaryAttack() > 0 ? emoji.get(DAGGER) + player.getSecondaryAttack() + "\n" : "")  +
                (!itemEffects.isEmpty() ? "_Item effects:_ " +
                itemEffects.stream()
                        .map(PlayerEffect::toString)
                        .collect(Collectors.joining("\n", "\n", "\n")) : "") +
                (!expirableEffects.isEmpty() ? "_Expirable effects:_ " +
                player.getEffects().stream()
                                .filter(effect -> !effect.isPermanent())
                                .map(effect -> effect + ", expires in " + ((Expirable) effect).getTurnsLasts() + " turns")
                                .collect(Collectors.joining("\n", "\n", "\n")) : "") +
                emoji.get(STONKS) + ": " + player.getXp() + " xp " + generateXpBar(player.getXp(), player.getPlayerLevel(), player.getNextLevelXp());

    }
}
