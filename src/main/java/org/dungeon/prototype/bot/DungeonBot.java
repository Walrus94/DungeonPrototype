package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.TurnMonsterRoomUpdate;
import org.dungeon.prototype.annotations.aspect.TurnUpdate;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.EmptyRoom;
import org.dungeon.prototype.model.room.content.HealthShrine;
import org.dungeon.prototype.model.room.content.ManaShrine;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.service.BattleService;
import org.dungeon.prototype.service.KeyboardService;
import org.dungeon.prototype.service.MessageService;
import org.dungeon.prototype.service.MonsterService;
import org.dungeon.prototype.service.PlayerLevelService;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.inventory.InventoryService;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.room.RoomService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.objects.Privacy;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.dungeon.prototype.properties.CallbackType.ATTACK;
import static org.dungeon.prototype.properties.CallbackType.BOOTS;
import static org.dungeon.prototype.properties.CallbackType.GLOVES;
import static org.dungeon.prototype.properties.CallbackType.HEAD;
import static org.dungeon.prototype.properties.CallbackType.INVENTORY;
import static org.dungeon.prototype.properties.CallbackType.ITEM_INVENTORY;
import static org.dungeon.prototype.properties.CallbackType.ITEM_INVENTORY_EQUIP;
import static org.dungeon.prototype.properties.CallbackType.ITEM_INVENTORY_UN_EQUIP;
import static org.dungeon.prototype.properties.CallbackType.LEFT_HAND;
import static org.dungeon.prototype.properties.CallbackType.MAP;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_BUY_MENU;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_DISPLAY_ITEM;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_MENU;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_PRICE;
import static org.dungeon.prototype.properties.CallbackType.RIGHT_HAND;
import static org.dungeon.prototype.properties.CallbackType.VEST;
import static org.dungeon.prototype.util.FileUtil.getRoomAsset;
import static org.dungeon.prototype.util.LevelUtil.getDirectionSwitchByCallBackData;
import static org.dungeon.prototype.util.LevelUtil.getErrorMessageByCallBackData;
import static org.dungeon.prototype.util.LevelUtil.getMonsterKilledRoomType;
import static org.dungeon.prototype.util.LevelUtil.getNextPointInDirection;
import static org.dungeon.prototype.util.LevelUtil.printMap;
import static org.dungeon.prototype.util.RoomGenerationUtils.getMonsterRoomTypes;

@Slf4j
@Component
public class DungeonBot extends AbilityBot {
    private final Map<Long, Integer> lastMessageByChat;
    private final Map<Long, Boolean> awaitingNickname;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private MonsterService monsterService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private BattleService battleService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private KeyboardService keyboardService;
    @Autowired
    private MessageService messageService;

    @Autowired
    public DungeonBot(@Value("${bot.token}") String botToken, @Value("${bot.username}") String botUsername) {
        super(botToken, botUsername);
        lastMessageByChat = new HashMap<>();
        awaitingNickname = new HashMap<>();
    }

    @Override
    public long creatorId() {
        return 151557417L;
    }

    public Ability start() {
        return Ability.builder()
                .name("start")
                .info("Starts bot")
                .locality(Locality.USER)
                .privacy(Privacy.PUBLIC)
                .action(this::processStartAction)
                .build();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            val chatId = update.getMessage().getChatId();
            if (awaitingNickname.containsKey(chatId) && awaitingNickname.get(chatId)) {
                val nickname = update.getMessage().getText();
                val player = playerService.addNewPlayer(chatId, nickname);
                log.debug("Player generated: {}", player);
                awaitingNickname.put(chatId, false);
                sendStartMessage(chatId, nickname, false);
            } else {
                super.onUpdateReceived(update);
            }
        } else if (update.hasCallbackQuery()) {
            if (!handleCallbackQuery(update)) {
                super.onUpdateReceived(update);
            }
        } else {
            super.onUpdateReceived(update);
        }
    }

    private boolean handleCallbackQuery(Update update) {
        val callbackQuery = update.getCallbackQuery();
        val callBackQueryId = callbackQuery.getId();
        val callData = callbackQuery.getData();
        val callBackData = keyboardService.getCallbackType(callData)
                .orElse(CallbackType.DEFAULT);
        val chatId = callbackQuery.getMessage().getChatId() == null ?
                update.getMessage().getChatId() :
                callbackQuery.getMessage().getChatId();

        var player = playerService.getPlayer(chatId);

        return switch (callBackData) {
            case START_GAME -> {
                answerCallbackQuery(callBackQueryId);
                yield startNewGame(chatId);
            }
            case CONTINUE_GAME -> {
                answerCallbackQuery(callBackQueryId);
                val level = levelService.getLevel(chatId);
                val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
                yield continueGame(player, level, currentRoom, chatId);
            }
            case LEFT, RIGHT, FORWARD, BACK -> {
                answerCallbackQuery(callBackQueryId);
                val level = levelService.getLevel(chatId);
                val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
                if (Objects.isNull(currentRoom)) {
                    log.error("Unable to find room with id {}", player.getCurrentRoomId());
                    yield false;
                }
                val newDirection = getDirectionSwitchByCallBackData(player.getDirection(), callBackData);
                val errorMessage = getErrorMessageByCallBackData(callBackData);
                if (!currentRoom.getAdjacentRooms().containsKey(newDirection) || !currentRoom.getAdjacentRooms().get((newDirection))) {
                    log.error(errorMessage);
                    yield false;
                }
                val nextRoom = level.getRoomByCoordinates(getNextPointInDirection(currentRoom.getPoint(), newDirection));
                if (nextRoom == null) {
                    log.error(errorMessage);
                    yield false;
                }
                player.setCurrentRoom(nextRoom.getPoint());
                player.setCurrentRoomId(nextRoom.getId());
                player.setDirection(newDirection);
                level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
                log.debug("Moving to {} door: {}, updated direction: {}", callBackData.toString().toLowerCase(), nextRoom.getPoint(), player.getDirection());
                yield moveToRoom(chatId, player, level, nextRoom);
            }
            case ATTACK, SECONDARY_ATTACK -> {
                answerCallbackQuery(callBackQueryId);
                val level = levelService.getLevel(chatId);
                val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
                yield attack(chatId, player, level, currentRoom, callBackData.equals(ATTACK));
            }
            case TREASURE_OPEN -> {
                answerCallbackQuery(callBackQueryId);
                val level = levelService.getLevel(chatId);
                val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
                yield openTreasure(player, level, currentRoom, chatId);
            }
            case SHRINE -> {
                val level = levelService.getLevel(chatId);
                val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
                answerCallbackQuery(callBackQueryId);
                yield shrineRefill(player, level, currentRoom, chatId);
            }
            case MERCHANT_BUY_MENU, MERCHANT_BUY_MENU_BACK -> {
                val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
                answerCallbackQuery(callBackQueryId);
                yield openMerchantBuyMenu(player, currentRoom, chatId);
            }
            case MERCHANT_SELL_MENU, MERCHANT_SELL_MENU_BACK -> {
                answerCallbackQuery(callBackQueryId);
                yield openMerchantSellMenu(player, chatId);
            }
            case MAP -> {
                answerCallbackQuery(callBackQueryId);
                val level = levelService.getLevel(chatId);
                yield sendOrUpdateMapMessage(player, level, chatId);
            }
            case INVENTORY -> {
                answerCallbackQuery(callBackQueryId);
                yield sendOrUpdateInventoryMessage(player, chatId);
            }
            case MENU_BACK -> {
                answerCallbackQuery(callBackQueryId);
                val level = levelService.getLevel(chatId);
                val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
                yield updateRoomAndSendMessage(level, currentRoom, player, chatId);
            }
            case NEXT_LEVEL -> {
                answerCallbackQuery(callBackQueryId);
                yield nextLevel(player, chatId);
            }
            case TREASURE_GOLD_COLLECTED -> {
                answerCallbackQuery(callBackQueryId);
                val level = levelService.getLevel(chatId);
                val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
                yield collectTreasureGold(player, level, currentRoom, chatId);
            }
            case COLLECT_ALL -> {
                answerCallbackQuery(callBackQueryId);
                val level = levelService.getLevel(chatId);
                val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
                yield collectAllTreasure(player, level ,currentRoom, chatId);
            }
            case RESTORE_ARMOR -> {
                answerCallbackQuery(callBackQueryId);
                yield restoreArmor(chatId, player);
            }
            case SHARPEN_WEAPON -> {
                answerCallbackQuery(callBackQueryId);
                yield sharpenWeapon(chatId);
            }
            default -> {
                if (callData.startsWith("btn_treasure_item_collected_")) {
                    val itemId = callData.replaceFirst("^" + "btn_treasure_item_collected_", "");
                    answerCallbackQuery(callBackQueryId);
                    yield collectTreasureItem(chatId, itemId);
                }
                if (callData.startsWith("btn_inventory_display_")) {
                    if (callData.startsWith("btn_inventory_display_item_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_item_", "");
                        answerCallbackQuery(callBackQueryId);
                        yield openInventoryItemInfo(itemId, INVENTORY, chatId);
                    }

                    if (callData.startsWith("btn_inventory_display_head_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_head_", "");
                        answerCallbackQuery(callBackQueryId);
                        yield openInventoryItemInfo(itemId, INVENTORY, chatId, HEAD);
                    }
                    if (callData.startsWith("btn_inventory_display_vest_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_vest_", "");
                        answerCallbackQuery(callBackQueryId);
                        yield openInventoryItemInfo(itemId, INVENTORY, chatId, VEST);
                    }
                    if (callData.startsWith("btn_inventory_display_gloves_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_gloves_", "");
                        answerCallbackQuery(callBackQueryId);
                        yield openInventoryItemInfo(itemId, INVENTORY, chatId, GLOVES);
                    }
                    if (callData.startsWith("btn_inventory_display_boots_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_boots_", "");
                        answerCallbackQuery(callBackQueryId);
                        yield openInventoryItemInfo(itemId, INVENTORY, chatId, BOOTS);
                    }
                    if (callData.startsWith("btn_inventory_display_primary_weapon_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_primary_weapon_", "");
                        answerCallbackQuery(callBackQueryId);
                        yield openInventoryItemInfo(itemId, INVENTORY, chatId, RIGHT_HAND);
                    }
                    if (callData.startsWith("btn_inventory_display_secondary_weapon_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_secondary_weapon_", "");
                        answerCallbackQuery(callBackQueryId);
                        yield openInventoryItemInfo(itemId, INVENTORY, chatId, LEFT_HAND);
                    }
                }
                if (callData.startsWith("btn_inventory_item_")) {
                    if (callData.startsWith("btn_inventory_item_equip_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_item_equip_", "");
                        answerCallbackQuery(callBackQueryId);
                        yield equipItem(player, itemId, chatId);
                    }
                    if (callData.startsWith("btn_inventory_item_un_equip_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_item_un_equip_", "");
                        answerCallbackQuery(callBackQueryId);
                        yield unEquipItem(player, itemId, chatId);
                    }
                }
                if (callData.startsWith("btn_merchant_")) {

                    if (callData.startsWith("btn_merchant_list_item_sell_")) {
                        val itemId = callData.replaceFirst("^" + "btn_merchant_list_item_sell_", "");
                        answerCallbackQuery(callBackQueryId);
                        yield openMerchantSellItem(itemId, chatId);
                    }

                    if (callData.startsWith("btn_merchant_list_item_buy_")) {
                        val itemId = callData.replaceFirst("^" + "btn_merchant_list_item_buy_", "");
                        answerCallbackQuery(callBackQueryId);
                        yield openMerchantBuyItem(itemId, chatId);
                    }
                    if (callData.startsWith("btn_merchant_sell_")) {
                        val itemId = callData.replaceFirst("^" + "btn_merchant_sell_", "");
                        answerCallbackQuery(callBackQueryId);
                        yield sellItem(player, itemId, chatId);
                    }
                    if (callData.startsWith("btn_merchant_buy_")) {
                        val itemId = callData.replaceFirst("^" + "btn_merchant_buy_", "");
                        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
                        answerCallbackQuery(callBackQueryId);
                        yield buyItem(player, currentRoom, itemId, chatId);
                    }

                    if (callData.startsWith("btn_merchant_display_")) {
                        if (callData.startsWith("btn_merchant_display_item_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_item_", "");
                            answerCallbackQuery(callBackQueryId);
                            yield openInventoryItemInfo(itemId, MERCHANT_SELL_MENU, chatId);
                        }
                        if (callData.startsWith("btn_merchant_display_head_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_head_", "");
                            answerCallbackQuery(callBackQueryId);
                            yield openInventoryItemInfo(itemId, MERCHANT_SELL_MENU, chatId, HEAD);
                        }
                        if (callData.startsWith("btn_merchant_display_vest_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_vest_", "");
                            answerCallbackQuery(callBackQueryId);
                            yield openInventoryItemInfo(itemId, MERCHANT_SELL_MENU, chatId, VEST);
                        }
                        if (callData.startsWith("btn_merchant_display_gloves_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_gloves_", "");
                            answerCallbackQuery(callBackQueryId);
                            yield openInventoryItemInfo(itemId, MERCHANT_SELL_MENU, chatId, GLOVES);
                        }
                        if (callData.startsWith("btn_merchant_display_boots_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_boots_", "");
                            answerCallbackQuery(callBackQueryId);
                            yield openInventoryItemInfo(itemId, MERCHANT_SELL_MENU, chatId, BOOTS);
                        }
                        if (callData.startsWith("btn_merchant_display_primary_weapon_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_primary_weapon_", "");
                            answerCallbackQuery(callBackQueryId);
                            yield openInventoryItemInfo(itemId, MERCHANT_SELL_MENU, chatId, RIGHT_HAND);
                        }
                        if (callData.startsWith("btn_merchant_display_secondary_weapon_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_secondary_weapon_", "");
                            answerCallbackQuery(callBackQueryId);
                            yield openInventoryItemInfo(itemId, MERCHANT_SELL_MENU, chatId, LEFT_HAND);
                        }
                    }
                }

                if (callData.startsWith("btn_player_attribute_upgrade_")) {
                    val playerAttribute = PlayerAttribute.fromValue(callData.replaceFirst("^" + "btn_player_attribute_upgrade_", ""));
                    val level = levelService.getLevel(chatId);
                    val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
                    yield upgradePlayerAttribute(player, level, currentRoom, playerAttribute, chatId);
                }
                answerCallbackQuery(callBackQueryId);
                yield false;
            }
        };
    }

    private void answerCallbackQuery(String callbackQueryId) {
        val answerCallbackQuery = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .showAlert(false)  // Set to true if you want an alert rather than a toast
                .build();
        try {
            execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            log.error("Unable to answer callback: {}", callbackQueryId);
        }
    }

    private boolean openMerchantSellMenu(Player player, Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Gold: " + player.getGold() +"\nSell your items:")
                .replyMarkup(keyboardService.getInventoryReplyMarkup(player.getInventory(), MERCHANT_SELL_DISPLAY_ITEM, MERCHANT_SELL_PRICE, MERCHANT_SELL_PRICE, MERCHANT_BUY_MENU))
                .build();
        return sendMessage(message, chatId);
    }

    private boolean openMerchantSellItem(String itemId, Long chatId) {
        val item = itemService.findItem(chatId, itemId);
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(messageService.getMerchantSellItemInfoMessageCaption(item))
                .replyMarkup(keyboardService.getMerchantSellItemInfoReplyMarkup(item))
                .build();
        return sendMessage(message, chatId);
    }

    private boolean sellItem(Player player, String itemId, Long chatId) {
        val item = itemService.findItem(chatId, itemId);
        val inventory = player.getInventory();
        inventory.remove(Stream.concat(player.getInventory().getItems().stream(),
                        Stream.concat(player.getInventory().getArmorSet().getArmorItems().stream(),
                                player.getInventory().getArmorSet().getArmorItems().stream()))
                .filter(Objects::nonNull)
                .filter(item::equals)
                .findFirst().orElse(null));
        player.addGold(item.getSellingPrice());
        inventoryService.saveOrUpdateInventory(inventory);
        playerService.updatePlayer(player);
        return openMerchantSellMenu(player, chatId);
    }

    private boolean openMerchantBuyMenu(Player player, Room currentRoom, Long chatId) {
        val merchant = (Merchant) currentRoom.getRoomContent();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Gold: " + player.getGold() + "\nAvailable items:")
                .replyMarkup(keyboardService.getMerchantBuyListReplyMarkup(merchant.getItems()))
                .build();
        return sendMessage(message,chatId);
    }

    private boolean openMerchantBuyItem(String itemId, Long chatId) {
        val item = itemService.findItem(chatId, itemId);
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(messageService.getMerchantSellItemInfoMessageCaption(item))
                .replyMarkup(keyboardService.getMerchantBuyItemInfoReplyMarkup(item))
                .build();
        return sendMessage(message, chatId);
    }

    private boolean buyItem(Player player, Room currentRoom, String itemId, Long chatId) {
        if (player.getInventory().isFull()) {
            log.warn("Inventory is full!");
            return false;
        }
        val item = itemService.findItem(chatId, itemId);
        if (player.getGold() < item.getBuyingPrice()) {
            log.warn("Not enough money!");
            return false;
        }
        player.getInventory().addItem(item);
        ((Merchant) currentRoom.getRoomContent()).getItems().remove(item);
        player.removeGold(item.getSellingPrice());
        roomService.saveOrUpdateRoom(currentRoom);
        inventoryService.saveOrUpdateInventory(player.getInventory());
        return openMerchantBuyMenu(player, currentRoom, chatId);
    }

    private boolean openInventoryItemInfo(String itemId, CallbackType inventoryType, Long chatId, CallbackType callbackType) {
        val item = itemService.findItem(chatId, itemId);
        SendMessage message = getInventoryItemInfoMessage(item, inventoryType, chatId, messageService.formatItemType(callbackType));
        return sendMessage(message, chatId);}

    private boolean equipItem(Player player, String itemId, Long chatId) {
        val item = itemService.findItem(chatId, itemId);
        val inventory = player.getInventory();
        if (inventoryService.equipItem(inventory, item)) {
            return sendOrUpdateInventoryMessage(player, chatId);
        }
        return false;
    }

    private boolean openInventoryItemInfo(String itemId, CallbackType inventoryType, Long chatId) {
        val item= itemService.findItem(chatId, itemId);
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(messageService.getInventoryUnEquippedItemInfoMessageCaption(item))
                .replyMarkup(keyboardService.getInventoryItemInfoReplyMarkup(item, inventoryType))
                .build();
        return sendMessage(message, chatId);
    }

    private boolean unEquipItem(Player player, String itemId, Long chatId) {
        val item= itemService.findItem(chatId, itemId);
        val inventory = player.getInventory();
        if ((inventory.getMaxItems().equals(inventory.getItems().size()))) {
            //TODO implement prompt
            sendOrUpdateInventoryMessage(player, chatId);
        }
        if (inventoryService.unEquipItem(item, inventory)) {
            return sendOrUpdateInventoryMessage(player, chatId);
        }
        return false;
    }

    private boolean sharpenWeapon(Long chatId) {
        //TODO: implement
        return true;
    }

    private boolean restoreArmor(Long chatId, Player player) {
        player.restoreArmor();
        return updatePlayerAndSendMessage(player, chatId);
    }

    private boolean sendOrUpdateInventoryMessage(Player player, Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .replyMarkup(keyboardService.getInventoryReplyMarkup(player.getInventory(), ITEM_INVENTORY, ITEM_INVENTORY_UN_EQUIP, ITEM_INVENTORY_EQUIP, MAP))
                .text("Inventory")
                .build();
        return sendMessage(message, chatId);
    }

    public SendMessage getInventoryItemInfoMessage(Item item, CallbackType inventoryType, Long chatId, String itemType) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(messageService.getInventoryEquippedItemInfoMessageCaption(item, itemType))
                .parseMode("Markdown")
                .replyMarkup(keyboardService.getEquippedItemInfoReplyMarkup(inventoryType, item.getSellingPrice()))
                .build();
    }

    private boolean openTreasure(Player player, Level level, Room currentRoom, Long chatId) {
        if (!RoomType.TREASURE.equals(currentRoom.getRoomContent().getRoomType())) {
            log.error("No treasure to collect!");
            return false;
        }
        val treasure = (Treasure) currentRoom.getRoomContent();
        if (treasure.getGold() == 0 && treasure.getItems().isEmpty()) {
            level.updateRoomType(currentRoom.getPoint(), RoomType.TREASURE_LOOTED);
            log.debug("Treasure looted!");
            return updateRoomAndSendMessage(level, currentRoom, player, chatId);
        }
        return sendTreasureMessage(chatId, treasure);
    }

    private boolean collectTreasureGold(Player player, Level level, Room currentRoom, Long chatId) {
        val treasure = (Treasure) currentRoom.getRoomContent();

        player.addGold(treasure.getGold());
        treasure.setGold(0);
        roomService.saveOrUpdateRoom(currentRoom);
        if (treasure.getGold() == 0 && treasure.getItems().isEmpty()) {
            level.updateRoomType(currentRoom.getPoint(), RoomType.TREASURE_LOOTED);
            log.debug("Treasure looted!");
            return updateRoomAndSendMessage(level, currentRoom, player, chatId);
        }
        return sendTreasureMessage(chatId, treasure);
    }

    private boolean collectTreasureItem(Long chatId, String itemId) {
        val player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);
        val treasure = (Treasure) currentRoom.getRoomContent();

        val items = treasure.getItems();
        val collectedItem = items.stream().filter(item -> item.getId().equals(itemId)).findFirst().orElseGet(() -> {
            log.error("No item with id {} found for chat {}!", itemId, chatId);
            return null;
        });
        if (Objects.isNull(collectedItem)) {
            return false;
        }
        if (player.getInventory().addItem(collectedItem)) {
            items.remove(collectedItem);
            treasure.setItems(items);
            roomService.saveOrUpdateRoom(currentRoom);
        } else {
            log.info("No room in inventory!");
            return sendTreasureMessage(chatId, treasure);
        }
        if (treasure.getGold() == 0 && treasure.getItems().isEmpty()) {
            level.updateRoomType(point, RoomType.TREASURE_LOOTED);
            log.info("Treasure looted!");
            return updateRoomAndSendMessage(level, currentRoom, player, chatId);
        }
        return sendTreasureMessage(chatId, treasure);
    }

    private boolean collectAllTreasure(Player player, Level level, Room currentRoom, Long chatId) {
        val treasure = (Treasure) currentRoom.getRoomContent();
        log.debug("Treasure contents - gold: {}, items: {}", treasure.getGold(), treasure.getItems());
        val items = treasure.getItems();
        val gold = treasure.getGold();

        player.addGold(gold);
        treasure.setGold(0);
        if (!items.isEmpty() && !player.getInventory().addItems(items)) {
            log.info("No room in the inventory!");
            playerService.updatePlayer(player);
            levelService.saveOrUpdateLevel(level);
            roomService.saveOrUpdateRoom(currentRoom);
            return sendTreasureMessage(chatId, treasure);
        } else {
            treasure.setItems(Collections.emptySet());
            inventoryService.saveOrUpdateInventory(player.getInventory());
        }

        level.updateRoomType(currentRoom.getPoint(), RoomType.TREASURE_LOOTED);
        level = levelService.saveOrUpdateLevel(level);
        currentRoom = level.getRoomByCoordinates(currentRoom.getPoint());
        log.debug("Treasure looted!");
        return updateRoomAndSendMessage(level, currentRoom, player, chatId);
    }

    private boolean sendTreasureMessage(Long chatId, Treasure treasure) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Treasure:")
                .replyMarkup(keyboardService.getTreasureContentReplyMarkup(treasure))
                .build();
        return sendMessage(message, chatId);
    }

    private boolean shrineRefill(Player player, Level level, Room currentRoom, Long chatId) {
        if (Objects.isNull(currentRoom)) {
            log.error("Unable to find room by Id:, {}", player.getCurrentRoomId());
            return false;
        }
        //TODO: fix shrines working
        if (!RoomType.HEALTH_SHRINE.equals(currentRoom.getRoomContent().getRoomType()) &&
                !RoomType.MANA_SHRINE.equals(currentRoom.getRoomContent().getRoomType())) {
            log.error("No shrine to use!");
            return false;
        }
        level.updateRoomType(currentRoom.getPoint(), RoomType.SHRINE_DRAINED);
        if (currentRoom.getRoomContent().getRoomType().equals(RoomType.HEALTH_SHRINE)) {
            player.addEffects(List.of(((HealthShrine) currentRoom.getRoomContent()).getEffect()));
        }
        if (currentRoom.getRoomContent().getRoomType().equals(RoomType.MANA_SHRINE)) {
            player.addEffects(List.of(((ManaShrine) currentRoom.getRoomContent()).getEffect()));
        }
        return updateRoomAndSendMessage(level, currentRoom, player, chatId);
    }

    @TurnMonsterRoomUpdate
    private boolean attack(Long chatId, Player player, Level level, Room currentRoom, boolean isPrimaryAttack) {
        if (!getMonsterRoomTypes().contains(currentRoom.getRoomContent().getRoomType())) {
            log.error("No monster to attack!");
            return false;
        }
        if (isNull(level.getActiveMonster())) {
            level.setActiveMonster(((MonsterRoom) currentRoom.getRoomContent()).getMonster());
        }
        var monster = level.getActiveMonster();
        log.debug("Attacking monster: {}", monster);
        monster = battleService.playerAttacks(monster, player, isPrimaryAttack);
        monsterService.saveOrUpdateMonster(monster);

        if (monster.getHp() < 1) {
            log.debug("Monster killed!");
            val roomType = getMonsterKilledRoomType(currentRoom.getRoomContent().getRoomType());
            currentRoom.setRoomContent(new EmptyRoom(roomType));
            level.updateRoomType(currentRoom.getPoint(), roomType);
            val hasReachedNewLevel = player.addXp(monster.getXpReward());
            level.setActiveMonster(null);
            if (hasReachedNewLevel) {
                return sendLevelUpgradeMessage(player, chatId);
            }
            return updateRoomAndSendMessage(level, currentRoom, player, chatId);
        } else {
            player = battleService.monsterAttacks(player, monster);
        }
        if (player.getHp() < 0) {
            sendDeathMessage(chatId);
            itemService.dropCollection(chatId);
            return true;
        }
        return updateRoomAndSendMessage(level, currentRoom, player, chatId);
    }

    private boolean sendLevelUpgradeMessage(Player player, Long chatId) {
        SendMessage message = SendMessage.builder()
                .text("New level reached! Choose upgrade:")
                .replyMarkup(keyboardService.getNewLevelUpgradeReplyMarkup(player))
                .build();
        return sendMessage(message, chatId);
    }

    private boolean upgradePlayerAttribute(Player player, Level level, Room currentRoom, PlayerAttribute playerAttribute, Long chatId) {
        player.getAttributes().put(playerAttribute, player.getAttributes().get(playerAttribute) + 1);
        return updateRoomAndSendMessage(level, currentRoom, player, chatId);
    }

    @TurnUpdate
    private boolean moveToRoom(Long chatId, Player player, Level level, Room nextRoom) {
        return updateRoomAndSendMessage(level, nextRoom, player, chatId);
    }

    private boolean startNewGame(Long chatId) {
        itemService.generateItems(chatId);
        var player = playerService.getPlayer(chatId);
        player.setMaxHp(90 + player.getAttributes().get(PlayerAttribute.STAMINA));
        player.setHp(player.getMaxHp());
        player.setXp(0L);
        player.setPlayerLevel(PlayerLevelService.getLevel(player.getXp()));
        player.setNextLevelXp(PlayerLevelService.calculateXPForLevel(player.getPlayerLevel() + 1));
        player.setMaxMana(6 + player.getAttributes().get(PlayerAttribute.MAGIC));
        player.setMana(player.getMaxMana());
        playerService.addDefaultInventory(player, chatId);
        val level = startLevel(chatId, player, 1);
        log.debug("Player loaded: {}", player);
        if (!updateRoomAndSendMessage(level, level.getStart(), player, chatId)) {
            return false;
        }
        log.debug("Player started level 1, current point: {}", level.getStart().getPoint());
        return true;
    }

    private boolean continueGame(Player player, Level level, Room currentRoom, Long chatId) {
        if (Objects.isNull(currentRoom)) {
            log.error("Couldn't find current room by id: {}", player.getCurrentRoomId());
            return false;
        }
        if (!updateRoomAndSendMessage(level, currentRoom, player, chatId)) {
            return false;
        }
        log.debug("Player continued level {}, current point: {}", level.getNumber(), player.getCurrentRoom());
        return true;
    }

    @NotNull
    private Level startLevel(Long chatId, Player player, Integer levelNumber) {
        return levelService.startNewLevel(chatId, player, levelNumber);
    }

    private boolean nextLevel(Player player, Long chatId) {
        val number = levelService.getLevelNumber(chatId) + 1;
        val level = startLevel(chatId, player, number);
        player.setCurrentRoom(level.getStart().getPoint());
        player.setCurrentRoomId(level.getStart().getId());
        player.setDirection(level.getStart().getAdjacentRooms().entrySet().stream()
                .filter(entry -> nonNull(entry.getValue()) && entry.getValue())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null));
        player.restoreArmor();
        if (!updateRoomAndSendMessage(level, level.getStart(), player, chatId)) {
            return false;
        }
        log.debug("Player started level {}, current point, {}", number, level.getStart().getPoint());
        return true;
    }

    private void sendDeathMessage(Long chatId) {
        val message = SendMessage.builder()
                .chatId(chatId)
                .text("You are dead!")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboardRow(List.of(InlineKeyboardButton.builder()
                                .text("Start again")
                                .callbackData("btn_start_game")
                                .build()))
                        .build())
                .build();
        lastMessageByChat.remove(chatId);
        levelService.remove(chatId);
        sendMessage(message, chatId);
    }

    private boolean updatePlayerAndSendMessage(Player player, Long chatId) {
        playerService.updatePlayer(player);
        val level = levelService.getLevel(chatId);//TODO: refactor
        val roomId = player.getCurrentRoomId();
        val room = roomService.getRoomByIdAndChatId(chatId, roomId);
        return sendRoomMessage(player, level, chatId, room);
    }

    private Boolean updateRoomAndSendMessage(Level level, Room room, Player player, long chatId) {
        playerService.updatePlayer(player);
        levelService.saveOrUpdateLevel(level);
        roomService.saveOrUpdateRoom(room);
        return sendRoomMessage(player, level, chatId, room);
    }

    private boolean sendRoomMessage(Player player, Level level, Long chatId, Room room) {
        ClassPathResource imgFile = new ClassPathResource(getRoomAsset(room.getRoomContent().getRoomType()));
        String caption;
        if (nonNull(level.getActiveMonster())) {
            caption = messageService.getRoomMessageCaption(player, level.getActiveMonster());
        } else {
            caption = messageService.getRoomMessageCaption(player);
        }
        val keyboardMarkup = keyboardService.getRoomInlineKeyboardMarkup(room, player);

        if (lastMessageByChat.containsKey(chatId)) {
            val messageId = lastMessageByChat.get(chatId);
            deleteMessage(chatId, messageId);
        }

        try (InputStream inputStream = imgFile.getInputStream()) {
            val inputFile = new InputFile(inputStream, imgFile.getFilename());
            val sendMessage = SendPhoto.builder()
                    .chatId(chatId)
                    .caption(caption)
                    .photo(inputFile)
                    .replyMarkup(keyboardMarkup)
                    .build();
            try {
                val messageId = execute(sendMessage).getMessageId();
                lastMessageByChat.put(chatId, messageId);
                return true;
            } catch (TelegramApiException e) {
                log.error("Unable to send message: ", e);
                return false;
            }
        } catch (IOException e) {
            log.error("Error loading file: {}", e.getMessage());
            return false;
        }
    }

    private boolean processStartAction(MessageContext messageContext) {
        val chatId = messageContext.chatId();
        val nickName = messageContext.user().getUserName() == null ?
                messageContext.user().getFirstName() :
                messageContext.user().getUserName();
        if (!playerService.hasPlayer(chatId)) {
            return sendRegisterMessage(chatId, nickName);
        } else {
            val hasSavedGame = levelService.hasLevel(chatId);
            val nickname = playerService.getNicknameByChatId(chatId);
            return sendStartMessage(chatId, nickname.orElse(""), hasSavedGame);
        }
    }

    private boolean sendOrUpdateMapMessage(Player player, Level level, Long chatId) {
        val levelMap = printMap(level.getGrid(), level.getLevelMap(), player.getCurrentRoom(), player.getDirection());
        val sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(levelMap)
                .replyMarkup(keyboardService.getMapInlineKeyboardMarkup())
                .build();
        return sendMessage(sendMessage, chatId);
    }

    private boolean sendRegisterMessage(Long chatId, String nickName) {
        awaitingNickname.put(chatId, true);
        return sendPromptMessage(chatId, "Welcome to dungeon!\nPlease, enter nickname to register", nickName);
    }

    private boolean sendPromptMessage(Long chatId, String text, String suggested) {
        ForceReplyKeyboard keyboard = ForceReplyKeyboard.builder()
                .forceReply(false)
                .inputFieldPlaceholder(suggested)
                .build();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        return sendMessage(message, chatId);
    }

    private boolean sendStartMessage(Long chatId, String nickname, Boolean hasSavedGame) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(String.format("Welcome to dungeon, %s!", nickname))
                .replyMarkup(keyboardService.getStartInlineKeyboardMarkup(hasSavedGame))
                .build();
        return sendMessage(message, chatId);
    }

    private boolean sendMessage(SendMessage message, Long chatId) {
        try {
            val messageId = execute(message).getMessageId();
            if (messageId == -1) {
                return false;
            }
            if (lastMessageByChat.containsKey(chatId)) {
                deleteMessage(chatId, lastMessageByChat.get(chatId));
            }
            lastMessageByChat.put(chatId, messageId);
            return true;
        } catch (TelegramApiException e) {
            log.error("Unable to send message: ", e);
            return false;
        }
    }

    private boolean deleteMessage(long chatId, Integer messageId) {
        val deleteMessage = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
        try {
            return execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Unable to edit message id:{}. {}", messageId, e);
            return false;
        }
    }
}
