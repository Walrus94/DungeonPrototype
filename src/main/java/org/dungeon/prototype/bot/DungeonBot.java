package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.Monster;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.inventory.ItemNamingService;
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

import static org.dungeon.prototype.util.FileUtil.getRoomAsset;
import static org.dungeon.prototype.util.KeyboardUtil.*;
import static org.dungeon.prototype.util.LevelUtil.*;
import static org.dungeon.prototype.util.MessageUtil.getRoomMessageCaption;

@Slf4j
@Component
public class DungeonBot extends AbilityBot {
    private final Map<Long, Monster> monstersByChat;
    private final Map<Long, Integer> lastMessageByChat;

    private final Map<Long, Boolean> awaitingNickname;

    @Autowired
    private ItemNamingService itemNamingService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private RoomService roomService;

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
        return switch (callData) {
            case "start_game" -> {
                answerCallbackQuery(callBackQueryId);
                yield startNewGame(chatId);
            }
            case "continue_game" -> {
                answerCallbackQuery(callBackQueryId);
                yield continueGame(chatId);
            }
            case "btn_left" -> {
                answerCallbackQuery(callBackQueryId);
                yield moveToLeftRoom(chatId);
            }
            case "btn_right" -> {
                answerCallbackQuery(callBackQueryId);
                yield moveToRightRoom(chatId);
            }
            case "btn_middle" -> {
                answerCallbackQuery(callBackQueryId);
                yield moveToMiddleRoom(chatId);
            }
            case "btn_turn_back" -> {
                answerCallbackQuery(callBackQueryId);
                yield moveBack(chatId);
            }
            case "btn_attack" -> {
                answerCallbackQuery(callBackQueryId);
                yield attack(chatId);
            }
            case "btn_collect" -> {
                answerCallbackQuery(callBackQueryId);
                yield collectTreasure(chatId);
            }
            case "btn_shrine_use" -> {
                answerCallbackQuery(callBackQueryId);
                yield shrineRefill(chatId);
            }
            case "btn_merchant" -> {
                answerCallbackQuery(callBackQueryId);
                yield openMerchant(chatId);
            }
            case "btn_menu" -> {
                answerCallbackQuery(callBackQueryId);
                yield sendOrUpdateMenuMessage(chatId);
            }
            case "btn_menu_back" -> {
                answerCallbackQuery(callBackQueryId);
                //TODO: fix END_ROOM have no Next Level button
                yield sendOrUpdateRoomMessage(chatId);
            }
            case "btn_next_level" -> {
                answerCallbackQuery(callBackQueryId);
                yield nextLevel(chatId);
            }
            default -> {
                answerCallbackQuery(callBackQueryId);
                yield false;
            }
        };
    }

    private boolean openMerchant(Long chatId) {
        //TODO: investigate webApp
        return true;
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

    private boolean shrineRefill(Long chatId) {
        var player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (Objects.isNull(currentRoom)) {
            log.error("Unable to find room by Id:, {}", player.getCurrentRoomId());
            return false;
        }
        //TODO: fix shrine using
        if (!RoomType.SHRINE.equals(currentRoom.getRoomContent().getRoomType())) {
            log.error("No shrine to use!");
            return false;
        }
        level.updateRoomType(point, RoomType.SHRINE_DRAINED);
        player.refillHp();
        player.refillMana();
        player = playerService.updatePlayer(player);
        roomService.saveOrUpdateRoom(currentRoom);
        levelService.updateLevel(level);
        val messageId = sendOrUpdateRoomMessage(currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private boolean attack(Long chatId) {
        var player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        var currentRoom = level.getRoomByCoordinates(point);
        if (!RoomType.MONSTER.equals(currentRoom.getRoomContent().getRoomType())) {
            log.error("No monster to attack!");
            return false;
        }
        if (!monstersByChat.containsKey(chatId)) {
            monstersByChat.put(chatId, (Monster) currentRoom.getRoomContent());
        }
        var monster = monstersByChat.get(chatId);
        log.debug("Attacking monster: {}", monster);
        monster.setHp(monster.getHp() - player.getAttack());

        if (monster.getHp() < 1) {
            log.debug("Monster killed!");
            level.updateRoomType(point, RoomType.MONSTER_KILLED);
            player.addXp(monster.getXpReward());
            monstersByChat.remove(chatId);
            roomService.saveOrUpdateRoom(currentRoom);
            levelService.updateLevel(level);
            playerService.updatePlayer(player);
            val messageId = sendOrUpdateRoomMessage(currentRoom, player, chatId);
            if (messageId == -1) {
                return false;
            }
            lastMessageByChat.put(chatId, messageId);
            return true;
        } else {
            if (player.getDefense() == 0) {
                player.decreaseHp(monster.getAttack());
            } else {
                if (monster.getAttack() > player.getDefense()) {
                    player.decreaseDefence(1);
                }
            }
        }
        if (player.getHp() < 0) {
            sendDeathMessage(chatId);
            return true;
        }
        roomService.saveOrUpdateRoom(currentRoom);
        levelService.updateLevel(level);
        playerService.updatePlayer(player);
        val messageId = sendOrUpdateRoomMessage(currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private boolean collectTreasure(Long chatId) {
        var player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val point = player.getCurrentRoom();
        var currentRoom = level.getRoomByCoordinates(point);
        if (!RoomType.TREASURE.equals(currentRoom.getRoomContent().getRoomType())) {
            log.error("No treasure to collect!");
            return false;
        }
        player.addGold(((Treasure)currentRoom.getRoomContent()).getReward());
        level.updateRoomType(point, RoomType.TREASURE_LOOTED);
        log.debug("Treasure looted!");
        playerService.updatePlayer(player);
        levelService.updateLevel(level);
        roomService.saveOrUpdateRoom(currentRoom);
        val messageId = sendOrUpdateRoomMessage(currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        return true;
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
            if (RoomType.MONSTER.equals(nextRoom.getRoomContent().getRoomType()) && !monstersByChat.containsKey(chatId)) {
                monstersByChat.put(chatId, (Monster) nextRoom.getRoomContent());
            }
            playerService.updatePlayer(player);
            level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
            levelService.updateLevel(level);
            log.debug("Moving back to {}, updated direction: {}", nextRoom.getPoint(), player.getDirection());
            val messageId = sendOrUpdateRoomMessage(nextRoom, player, chatId);
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
        if (RoomType.MONSTER.equals(nextRoom.getRoomContent().getRoomType()) && !monstersByChat.containsKey(chatId)) {
            monstersByChat.put(chatId, (Monster) nextRoom.getRoomContent());
        }
        playerService.updatePlayer(player);
        level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
        levelService.updateLevel(level);
        val messageId = sendOrUpdateRoomMessage(nextRoom, player, chatId);
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
            if (RoomType.MONSTER.equals(nextRoom.getRoomContent().getRoomType()) && !monstersByChat.containsKey(chatId)) {
                monstersByChat.put(chatId, (Monster) nextRoom.getRoomContent());
            }
            playerService.updatePlayer(player);
            level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
            levelService.updateLevel(level);
            val messageId = sendOrUpdateRoomMessage(nextRoom, player, chatId);
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
            if (RoomType.MONSTER.equals(nextRoom.getRoomContent().getRoomType()) && !monstersByChat.containsKey(chatId)) {
                monstersByChat.put(chatId, (Monster) nextRoom.getRoomContent());
            }
            playerService.updatePlayer(player);
            level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
            levelService.updateLevel(level);
            val messageId = sendOrUpdateRoomMessage(nextRoom, player, chatId);
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

    private boolean startNewGame(Long chatId) {
        val level = startLevel(chatId, 1);
        val direction = level.getStart().getAdjacentRooms().entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()) && entry.getValue())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);

        var player = playerService.getPlayer(chatId);
        player.setDirection(direction);
        player.setCurrentRoom(level.getStart().getPoint());
        player.setCurrentRoomId(level.getStart().getId());
        log.debug("Player loaded: {}", player);
        val messageId = sendOrUpdateRoomMessage(level.getStart(), player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        playerService.updatePlayer(player);
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
        val messageId = sendOrUpdateRoomMessage(currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        log.debug("Player continued level {}, current point: {}", level.getNumber(), player.getCurrentRoom());
        return true;
    }

    private boolean nextLevel(Long chatId) {
        val number = levelService.getLevelNumber(chatId) + 1;
        val level = startLevel(chatId, number);
        var player = playerService.getPlayer(chatId);
        player.setCurrentRoom(level.getStart().getPoint());
        player.setCurrentRoomId(level.getStart().getId());
        player.setDirection(level.getStart().getAdjacentRooms().entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()) && entry.getValue())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null));
        val messageId = sendOrUpdateRoomMessage(level.getStart(), player, chatId);
        if (messageId == -1) {
            return false;
        }
        playerService.updatePlayer(player);
        lastMessageByChat.put(chatId, messageId);
        log.debug("Player started level {}, current point, {}", number, level.getStart().getPoint());
        return true;
    }

    @NotNull
    private Level startLevel(Long chatId, Integer levelNumber) {
        return levelService.saveNewLevel(chatId, levelNumber);
    }

    private void sendDeathMessage(Long chatId) {
        val message = SendMessage.builder()
                .chatId(chatId)
                .text("You are dead!")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboardRow(List.of(InlineKeyboardButton.builder()
                                .text("Start again")
                                .callbackData("start_game")
                                .build()))
                .build())
                .build();
        lastMessageByChat.remove(chatId);
        monstersByChat.remove(chatId);
        levelService.remove(chatId);
        sendMessage(message);
    }

    private boolean sendOrUpdateRoomMessage(Long chatId) {
        var player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        val messageId = sendOrUpdateRoomMessage(currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private Integer sendOrUpdateRoomMessage(Room room, Player player, long chatId) {
        ClassPathResource imgFile = new ClassPathResource(getRoomAsset(room.getRoomContent().getRoomType()));
        String caption;
        if (monstersByChat.containsKey(chatId)) {
            caption = getRoomMessageCaption(player, monstersByChat.get(chatId));
        } else {
            caption = getRoomMessageCaption(player);
        }
        val keyboardMarkup = getRoomInlineKeyboardMarkup(room, player.getDirection());

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

    private boolean sendOrUpdateMenuMessage(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val levelMap = printMap(level.getGrid(), level.getLevelMap(), player.getCurrentRoom(), player.getDirection());
        if (lastMessageByChat.containsKey(chatId)) {
            val messageId = lastMessageByChat.get(chatId);

            val deleteMessage = DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build();
            try {
                execute(deleteMessage);
            } catch (TelegramApiException e) {
                log.error("Unable to edit message id:{}. {}", messageId, e);
                return false;
            }
        }
        val sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(levelMap)
                .replyMarkup(getMenuInlineKeyboardMarkup())
                .build();
        val messageId = sendMessage(sendMessage);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId,messageId);
        return true;
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

    private void sendRegisterMessage(Long chatId, String nickName) {
        awaitingNickname.put(chatId, true);
        sendPromptMessage(chatId, "Welcome to dungeon!\nPlease, enter nickname to register", nickName);
    }

    private void sendPromptMessage(Long chatId, String text, String suggested) {
        ForceReplyKeyboard keyboard = ForceReplyKeyboard.builder()
                .forceReply(true)
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
                .replyMarkup(getStartInlineKeyboardMarkup(hasSavedGame))
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
}
