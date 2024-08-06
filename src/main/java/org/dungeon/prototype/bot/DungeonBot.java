package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.service.BattleService;
import org.dungeon.prototype.service.KeyboardService;
import org.dungeon.prototype.service.PlayerLevelService;
import org.dungeon.prototype.service.PlayerService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.nonNull;
import static org.dungeon.prototype.model.effect.attributes.MonsterEffectAttribute.MOVING;
import static org.dungeon.prototype.util.FileUtil.getRoomAsset;
import static org.dungeon.prototype.util.LevelUtil.getMonsterKilledRoomType;
import static org.dungeon.prototype.util.LevelUtil.getNextPointInDirection;
import static org.dungeon.prototype.util.LevelUtil.getOppositeDirection;
import static org.dungeon.prototype.util.LevelUtil.printMap;
import static org.dungeon.prototype.util.LevelUtil.turnLeft;
import static org.dungeon.prototype.util.LevelUtil.turnRight;
import static org.dungeon.prototype.util.MessageUtil.getRoomMessageCaption;
import static org.dungeon.prototype.util.RoomGenerationUtils.getMonsterRoomTypes;

@Slf4j
@Component
public class DungeonBot extends AbilityBot {
    private final Map<Long, Monster> monstersByChat;
    private final Map<Long, Integer> lastMessageByChat;
    private final Map<Long, Boolean> awaitingNickname;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private BattleService battleService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private KeyboardService keyboardService;

    @Autowired
    public DungeonBot(@Value("${bot.token}") String botToken, @Value("${bot.username}") String botUsername) {
        super(botToken, botUsername);
        lastMessageByChat = new HashMap<>();
        monstersByChat = new HashMap<>();
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
        val chatId = callbackQuery.getMessage().getChatId() == null ?
                update.getMessage().getChatId() :
                callbackQuery.getMessage().getChatId();
        return switch (keyboardService.getCallbackType(callData)) {
            case START_GAME -> {
                answerCallbackQuery(callBackQueryId);
                yield startNewGame(chatId);
            }
            case CONTINUE_GAME -> {
                answerCallbackQuery(callBackQueryId);
                yield continueGame(chatId);
            }
            case LEFT -> {
                answerCallbackQuery(callBackQueryId);
                yield moveToLeftRoom(chatId);
            }
            case RIGHT -> {
                answerCallbackQuery(callBackQueryId);
                yield moveToRightRoom(chatId);
            }
            case FORWARD -> {
                answerCallbackQuery(callBackQueryId);
                yield moveToMiddleRoom(chatId);
            }
            case BACK -> {
                answerCallbackQuery(callBackQueryId);
                yield moveBack(chatId);
            }
            case ATTACK -> {
                answerCallbackQuery(callBackQueryId);
                yield attack(chatId, true);
            }
            case SECONDARY_ATTACK -> {
                answerCallbackQuery(callBackQueryId);
                yield attack(chatId, false);
            }
            case TREASURE_OPEN -> {
                answerCallbackQuery(callBackQueryId);
                yield openTreasure(chatId);
            }
            case SHRINE -> {
                answerCallbackQuery(callBackQueryId);
                yield shrineRefill(chatId);
            }
            case MERCHANT -> {
                answerCallbackQuery(callBackQueryId);
                yield openMerchant(chatId);
            }
            case MENU -> {
                answerCallbackQuery(callBackQueryId);
                yield sendOrUpdateMenuMessage(chatId);
            }
            case INVENTORY -> {
                answerCallbackQuery(callBackQueryId);
                yield sendOrUpdateInventoryMessage(chatId);
            }
            case MENU_BACK -> {
                answerCallbackQuery(callBackQueryId);
                yield sendOrUpdateRoomMessage(chatId);
            }
            case NEXT_LEVEL -> {
                answerCallbackQuery(callBackQueryId);
                yield nextLevel(chatId);
            }
            case TREASURE_GOLD_COLLECTED -> {
                answerCallbackQuery(callBackQueryId);
                yield collectTreasureGold(chatId);
            }
            case COLLECT_ALL -> {
                answerCallbackQuery(callBackQueryId);
                yield collectAllTreasure(chatId);
            }
            case RESTORE_ARMOR -> {
                answerCallbackQuery(callBackQueryId);
                yield restoreArmor(chatId);
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
                answerCallbackQuery(callBackQueryId);
                yield false;
            }
        };
    }

    private boolean sharpenWeapon(Long chatId) {
        //TODO: implement
        return true;
    }

    private boolean restoreArmor(Long chatId) {
        val player = playerService.getPlayer(chatId);
        player.restoreArmor();
        playerService.updatePlayer(player);
        return sendOrUpdateRoomMessage(chatId);
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

    private boolean sendOrUpdateInventoryMessage(Long chatId) {
        val player = playerService.getPlayer(chatId);
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .replyMarkup(keyboardService.getInventoryReplyMarkup(player))
                .text("Inventory")
                .build();
        val messageId = sendMessage(message);
        if (messageId == -1) {
            return false;
        }
        deleteMessage(chatId, lastMessageByChat.get(chatId));
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private boolean openTreasure(Long chatId) {
        var player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        var currentRoom = level.getRoomByCoordinates(point);
        if (!RoomType.TREASURE.equals(currentRoom.getRoomContent().getRoomType())) {
            log.error("No treasure to collect!");
            return false;
        }
        val treasure = (Treasure) currentRoom.getRoomContent();
        if (treasure.getGold() == 0 && treasure.getItems().isEmpty()) {
            level.updateRoomType(point, RoomType.TREASURE_LOOTED);
            log.debug("Treasure looted!");
            val messageId = updateRoomAndSendMessage(level, currentRoom, player, chatId);
            if (messageId == -1) {
                return false;
            }
            lastMessageByChat.put(chatId, messageId);
            return true;
        }
        val messageId = sendTreasureMessage(chatId, treasure);
        if (messageId == -1) {
            return false;
        }
        if (lastMessageByChat.containsKey(chatId)) {
            deleteMessage(chatId, lastMessageByChat.get(chatId));
        }
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private boolean collectAllTreasure(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);
        val treasure = (Treasure) currentRoom.getRoomContent();
        log.debug("Treasure contents - gold: {}, items: {}", treasure.getGold(), treasure.getItems());
        val items = treasure.getItems();
        val gold = treasure.getGold();

        player.addGold(gold);
        if (!items.isEmpty() && !player.getInventory().addItems(items)) {
            log.info("No room in the inventory!");
            treasure.setGold(0);
            playerService.updatePlayer(player);
            levelService.saveOrUpdateLevel(level);
            roomService.saveOrUpdateRoom(currentRoom);
            val messageId = sendTreasureMessage(chatId, treasure);
            if (messageId == -1) {
                return false;
            }
            lastMessageByChat.put(chatId, messageId);
            return true;
        }

        level.updateRoomType(point, RoomType.TREASURE_LOOTED);
        log.debug("Treasure looted!");
        val messageId = updateRoomAndSendMessage(level, currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        //TODO: verify it works
//        deleteMessage(chatId, lastMessageByChat.get(chatId));
//        lastMessageByChat.put(chatId, messageId);
        return true;
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
            val messageId = sendTreasureMessage(chatId, treasure);
            if (messageId == -1) {
                return false;
            }
            deleteMessage(chatId, lastMessageByChat.get(chatId));
            lastMessageByChat.put(chatId, messageId);
            return true;
        }
        if (treasure.getGold() == 0 && treasure.getItems().isEmpty()) {
            level.updateRoomType(point, RoomType.TREASURE_LOOTED);
            log.info("Treasure looted!");
            val messageId = updateRoomAndSendMessage(level, currentRoom, player, chatId);
            if (messageId == -1) {
                return false;
            }
            lastMessageByChat.put(chatId, messageId);
            return true;
        }
        val messageId = sendTreasureMessage(chatId, treasure);
        if (messageId == -1) {
            return false;
        }
        deleteMessage(chatId, lastMessageByChat.get(chatId));
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private boolean collectTreasureGold(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);
        val treasure = (Treasure) currentRoom.getRoomContent();

        player.addGold(treasure.getGold());
        treasure.setGold(0);
        roomService.saveOrUpdateRoom(currentRoom);
        if (treasure.getGold() == 0 && treasure.getItems().isEmpty()) {
            level.updateRoomType(point, RoomType.TREASURE_LOOTED);
            log.debug("Treasure looted!");
            val messageId = updateRoomAndSendMessage(level, currentRoom, player, chatId);
            if (messageId == -1) {
                return false;
            }
            lastMessageByChat.put(chatId, messageId);
            return true;
        }
        val messageId = sendTreasureMessage(chatId, treasure);
        if (messageId == -1) {
            return false;
        }
        deleteMessage(chatId, lastMessageByChat.get(chatId));
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private Integer sendTreasureMessage(Long chatId, Treasure treasure) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Treasure:")
                .replyMarkup(keyboardService.getTreasureContentReplyMarkup(treasure))
                .build();
        return sendMessage(message);
    }

    private boolean openMerchant(Long chatId) {
        //TODO: investigate webApp
        val player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);

        val merchant = (Merchant) currentRoom.getRoomContent();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Available items:")
                .replyMarkup(keyboardService.getMerchantReplyMarkup(merchant.getItems()))
                .build();
        sendMessage(message);
        return true;
    }

    private boolean shrineRefill(Long chatId) {
        var player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
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
        level.updateRoomType(point, RoomType.SHRINE_DRAINED);
        if (currentRoom.getRoomContent().getRoomType().equals(RoomType.HEALTH_SHRINE)) {
            player.refillHp();
        }
        if (currentRoom.getRoomContent().getRoomType().equals(RoomType.MANA_SHRINE)) {
            player.refillMana();
        }
        val messageId = updateRoomAndSendMessage(level, currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private boolean attack(Long chatId, boolean isPrimaryAttack) {
        var player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        var currentRoom = level.getRoomByCoordinates(point);
        if (!getMonsterRoomTypes().contains(currentRoom.getRoomContent().getRoomType())) {
            log.error("No monster to attack!");
            return false;
        }
        if (!monstersByChat.containsKey(chatId)) {
            monstersByChat.put(chatId, ((MonsterRoom) currentRoom.getRoomContent()).getMonster());
        }
        var monster = monstersByChat.get(chatId);
        log.debug("Attacking monster: {}", monster);
        monster = battleService.playerAttacks(monster, player, isPrimaryAttack);

        if (monster.getHp() < 1) {
            log.debug("Monster killed!");
            level.updateRoomType(point, getMonsterKilledRoomType(currentRoom.getRoomContent().getRoomType()));
            player.addXp(monster.getXpReward());
            monstersByChat.remove(chatId);
            val messageId = updateRoomAndSendMessage(level, currentRoom, player, chatId);
            if (messageId == -1) {
                return false;
            }
            lastMessageByChat.put(chatId, messageId);
            return true;
        } else {
            if (monster.getEffects().stream().noneMatch(monsterEffect -> MOVING.equals(monsterEffect.getAttribute()))) {
                if (Objects.isNull(monster.getCurrentAttack()) || !monster.getCurrentAttack().hasNext()) {
                    monster.setCurrentAttack(monster.getDefaultAttackPattern().listIterator());
                }
                val currentAttack = monster.getCurrentAttack().next();
                player = battleService.monsterAttacks(player, currentAttack);
            }
        }
        if (player.getHp() < 0) {
            sendDeathMessage(chatId);
            itemService.dropCollection(chatId);
            return true;
        }
        val messageId = updateRoomAndSendMessage(level, currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private boolean moveToMiddleRoom(Long chatId) {
        var player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (Objects.isNull(currentRoom)) {
            log.error("Unable to find room with id {}", player.getCurrentRoomId());
            return false;
        }
        if (!currentRoom.getAdjacentRooms().get((player.getDirection()))) {
            log.error("Middle door is locked!");
            return false;
        }
        val nextRoom = level.getRoomByCoordinates(getNextPointInDirection(currentRoom.getPoint(), player.getDirection()));

        player.setCurrentRoom(nextRoom.getPoint());
        player.setCurrentRoomId(nextRoom.getId());
        if (getMonsterRoomTypes().contains(nextRoom.getRoomContent().getRoomType()) && !monstersByChat.containsKey(chatId)) {
            val monster = ((MonsterRoom) nextRoom.getRoomContent()).getMonster();
            monster.setCurrentAttack(monster.getDefaultAttackPattern().listIterator());
            player.getInventory().getWeaponSet().getWeapons().stream()
                    .filter(Objects::nonNull)
                    .filter(weapon -> nonNull(weapon.getAdditionalFirstHit()))
                    .forEach(weapon -> battleService.firstHitAttacked(monster, weapon.getAdditionalFirstHit(), weapon.getAttributes().getWeaponAttackType()));
            monstersByChat.put(chatId, monster);
        }
        playerService.updatePlayer(player);
        level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
        val messageId = updateRoomAndSendMessage(level, nextRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        log.debug("Moving to middle door: {}, updated direction: {}", nextRoom.getPoint(), player.getDirection());
        return true;
    }

    private boolean moveToRightRoom(Long chatId) {
        var player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);
        val newDirection = turnRight(player.getDirection());
        if (currentRoom.getAdjacentRooms().containsKey(newDirection) && currentRoom.getAdjacentRooms().get(newDirection)) {
            val nextRoom = level.getRoomsMap().get(getNextPointInDirection(currentRoom.getPoint(), newDirection));
            if (nextRoom == null) {
                log.error("Right door is locked!");
                return false;
            }
            player.setCurrentRoom(nextRoom.getPoint());
            player.setCurrentRoomId(nextRoom.getId());
            player.setDirection(newDirection);
            if (getMonsterRoomTypes().contains(nextRoom.getRoomContent().getRoomType()) && !monstersByChat.containsKey(chatId)) {
                val monster = ((MonsterRoom) nextRoom.getRoomContent()).getMonster();
                monster.setCurrentAttack(monster.getDefaultAttackPattern().listIterator());
                player.getInventory().getWeaponSet().getWeapons().stream()
                        .filter(Objects::nonNull)
                        .filter(weapon -> nonNull(weapon.getAdditionalFirstHit()))
                        .forEach(weapon -> battleService.firstHitAttacked(monster, weapon.getAdditionalFirstHit(), weapon.getAttributes().getWeaponAttackType()));
                monstersByChat.put(chatId, monster);
            }
            level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
            val messageId = updateRoomAndSendMessage(level, nextRoom, player, chatId);
            if (messageId == -1) {
                return false;
            }
            lastMessageByChat.put(chatId, messageId);
            log.debug("Moving to right door: {}, updated direction: {}", nextRoom.getPoint(), player.getDirection());
            return true;
        } else {
            log.error("Right door is locked!");
            return false;
        }
    }

    private boolean moveToLeftRoom(Long chatId) {
        var player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);
        val newDirection = turnLeft(player.getDirection());
        if (currentRoom.getAdjacentRooms().containsKey(newDirection) && currentRoom.getAdjacentRooms().get(newDirection)) {
            val nextRoom = level.getRoomByCoordinates(getNextPointInDirection(point, newDirection));
            if (nextRoom == null) {
                log.error("Left door is locked!");
                return false;
            }
            player.setCurrentRoom(nextRoom.getPoint());
            player.setCurrentRoomId(nextRoom.getId());
            player.setDirection(newDirection);
            if (getMonsterRoomTypes().contains(nextRoom.getRoomContent().getRoomType()) && !monstersByChat.containsKey(chatId)) {
                val monster = ((MonsterRoom) nextRoom.getRoomContent()).getMonster();
                monster.setCurrentAttack(monster.getDefaultAttackPattern().listIterator());
                player.getInventory().getWeaponSet().getWeapons().stream()
                        .filter(Objects::nonNull)
                        .filter(weapon -> nonNull(weapon.getAdditionalFirstHit()))
                        .forEach(weapon -> battleService.firstHitAttacked(monster, weapon.getAdditionalFirstHit(), weapon.getAttributes().getWeaponAttackType()));
                monstersByChat.put(chatId, monster);
            }
            level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
            val messageId = updateRoomAndSendMessage(level, nextRoom, player, chatId);
            if (messageId == -1) {
                return false;
            }
            lastMessageByChat.put(chatId, messageId);
            log.debug("Moving to left door: {}, updated direction: {}", nextRoom.getPoint(), player.getDirection());
            return true;
        } else {
            log.error("Left door is locked!");
            return false;
        }
    }

    private boolean moveBack(Long chatId) {
        var player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);
        val newDirection = getOppositeDirection(player.getDirection());
        if (currentRoom.getAdjacentRooms().containsKey(newDirection) && currentRoom.getAdjacentRooms().get(newDirection)) {
            val nextRoom = level.getRoomsMap().get(getNextPointInDirection(currentRoom.getPoint(), newDirection));
            if (nextRoom == null) {
                log.error("Door on the back is locked!");
                return false;
            }
            player.setCurrentRoom(nextRoom.getPoint());
            player.setCurrentRoomId(nextRoom.getId());
            player.setDirection(newDirection);
            if (getMonsterRoomTypes().contains(nextRoom.getRoomContent().getRoomType()) && !monstersByChat.containsKey(chatId)) {
                val monster = ((MonsterRoom) nextRoom.getRoomContent()).getMonster();
                monster.setCurrentAttack(monster.getDefaultAttackPattern().listIterator());
                player.getInventory().getWeaponSet().getWeapons().stream()
                        .filter(Objects::nonNull)
                        .filter(weapon -> nonNull(weapon.getAdditionalFirstHit()))
                        .forEach(weapon -> battleService.firstHitAttacked(monster, weapon.getAdditionalFirstHit(), weapon.getAttributes().getWeaponAttackType()));
                monstersByChat.put(chatId, monster);
            }
            level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
            log.debug("Moving back to {}, updated direction: {}", nextRoom.getPoint(), player.getDirection());
            val messageId = updateRoomAndSendMessage(level, nextRoom, player, chatId);
            if (messageId == -1) {
                return false;
            }
            lastMessageByChat.put(chatId, messageId);
            return true;
        } else {
            log.error("Door on the back is locked!");
            return false;
        }
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
        val messageId = updateRoomAndSendMessage(level, level.getStart(), player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        log.debug("Player started level 1, current point: {}", level.getStart().getPoint());
        return true;
    }

    private boolean continueGame(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (Objects.isNull(currentRoom)) {
            log.error("Couldn't find current room by id: {}", player.getCurrentRoomId());
            return false;
        }
        val messageId = updateRoomAndSendMessage(level, currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        log.debug("Player continued level {}, current point: {}", level.getNumber(), player.getCurrentRoom());
        return true;
    }

    @NotNull
    private Level startLevel(Long chatId, Player player, Integer levelNumber) {
        return levelService.startNewLevel(chatId, player, levelNumber);
    }

    private boolean nextLevel(Long chatId) {
        val number = levelService.getLevelNumber(chatId) + 1;
        var player = playerService.getPlayer(chatId);
        val level = startLevel(chatId, player, number);
        player.setCurrentRoom(level.getStart().getPoint());
        player.setCurrentRoomId(level.getStart().getId());
        player.setDirection(level.getStart().getAdjacentRooms().entrySet().stream()
                .filter(entry -> nonNull(entry.getValue()) && entry.getValue())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null));
        player.restoreArmor();
        val messageId = updateRoomAndSendMessage(level, level.getStart(), player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
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
        monstersByChat.remove(chatId);
        levelService.remove(chatId);
        sendMessage(message);
    }

    //TODO: refactor to get rid of this method
    private boolean sendOrUpdateRoomMessage(Long chatId) {
        val level = levelService.getLevel(chatId);
        val player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        val messageId = updateRoomAndSendMessage(level, currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private Integer updateRoomAndSendMessage(Level level, Room room, Player player, long chatId) {
        playerService.updatePlayer(player);
        levelService.saveOrUpdateLevel(level);
        roomService.saveOrUpdateRoom(room);
        ClassPathResource imgFile = new ClassPathResource(getRoomAsset(room.getRoomContent().getRoomType()));
        String caption;
        if (monstersByChat.containsKey(chatId)) {
            caption = getRoomMessageCaption(player, monstersByChat.get(chatId));
        } else {
            caption = getRoomMessageCaption(player);
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
                return execute(sendMessage).getMessageId();
            } catch (TelegramApiException e) {
                log.error("Unable to send message: ", e);
                return -1;
            }
        } catch (IOException e) {
            log.error("Error loading file: {}", e.getMessage());
            return -1;
        }
    }

    private void processStartAction(MessageContext messageContext) {
        val chatId = messageContext.chatId();
        val nickName = messageContext.user().getUserName() == null ?
                messageContext.user().getFirstName() :
                messageContext.user().getUserName();
        if (!playerService.hasPlayer(chatId)) {
            sendRegisterMessage(chatId, nickName);
        } else {
            val hasSavedGame = levelService.hasLevel(chatId);
            val nickname = playerService.getNicknameByChatId(chatId);
            sendStartMessage(chatId, nickname.orElse(""), hasSavedGame);
        }
    }

    private boolean sendOrUpdateMenuMessage(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val levelMap = printMap(level.getGrid(), level.getLevelMap(), player.getCurrentRoom(), player.getDirection());
        if (lastMessageByChat.containsKey(chatId)) {
            val messageId = lastMessageByChat.get(chatId);
            deleteMessage(chatId, messageId);
        }
        val sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(levelMap)
                .replyMarkup(keyboardService.getMenuInlineKeyboardMarkup())
                .build();
        val messageId = sendMessage(sendMessage);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId,messageId);
        return true;
    }

    private void sendRegisterMessage(Long chatId, String nickName) {
        awaitingNickname.put(chatId, true);
        sendPromptMessage(chatId, "Welcome to dungeon!\nPlease, enter nickname to register", nickName);
    }

    private void sendPromptMessage(Long chatId, String text, String suggested) {
        ForceReplyKeyboard keyboard = ForceReplyKeyboard.builder()
                .forceReply(false)
                .inputFieldPlaceholder(suggested)
                .build();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        val messageId = sendMessage(message);
        if (messageId != -1) {
            lastMessageByChat.put(chatId, messageId);
        }
    }

    private void sendStartMessage(Long chatId, String nickname, Boolean hasSavedGame) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(String.format("Welcome to dungeon, %s!", nickname))
                .replyMarkup(keyboardService.getStartInlineKeyboardMarkup(hasSavedGame))
                .build();
        val messageId = sendMessage(message);
        if (messageId == -1) {
            return;
        }
        lastMessageByChat.put(chatId, messageId);
    }

    private Integer sendMessage(SendMessage message) {
        try {
            return execute(message).getMessageId();
        } catch (TelegramApiException e) {
            log.error("Unable to send message: ", e);
            return -1;
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
