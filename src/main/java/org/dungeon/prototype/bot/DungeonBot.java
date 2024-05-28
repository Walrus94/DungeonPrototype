package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Player;
import org.dungeon.prototype.model.room.Monster;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.Treasure;
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
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.dungeon.prototype.util.FileUtil.getRoomAsset;
import static org.dungeon.prototype.util.KeyboardUtil.getMenuInlineKeyboardMarkup;
import static org.dungeon.prototype.util.KeyboardUtil.getRoomInlineKeyboardMarkup;
import static org.dungeon.prototype.util.KeyboardUtil.getStartInlineKeyboardMarkup;
import static org.dungeon.prototype.util.LevelUtil.getOppositeDirection;
import static org.dungeon.prototype.util.LevelUtil.printMap;
import static org.dungeon.prototype.util.LevelUtil.turnLeft;
import static org.dungeon.prototype.util.LevelUtil.turnRight;
import static org.dungeon.prototype.util.MessageUtil.getRoomMessageCaption;

@Slf4j
@Component
public class DungeonBot extends AbilityBot {
    private static Map<Long, Level> currentLevelsMap;
    private static Map<Long, Monster> monstersByChat;
    private final Map<Long, Player> playersMap;
    private final Map<Long, Integer> lastMessageByChat;

    @Autowired
    public DungeonBot(@Value("${bot.token}") String botToken, @Value("${bot.username}") String botUsername) {
        super(botToken, botUsername);
        playersMap = db.getMap("players");
        lastMessageByChat = new HashMap<>();
        monstersByChat = new HashMap<>();
        currentLevelsMap = new HashMap<>();
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
        if (update.hasCallbackQuery()) {
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
        val nickname = callbackQuery.getFrom().getUserName() == null ?
                callbackQuery.getFrom().getFirstName() :
                "@" +callbackQuery.getFrom().getUserName();
        val chatId = callbackQuery.getMessage().getChatId();
        return switch (callData) {
            case "start_game" -> {
                answerCallbackQuery(callBackQueryId);
                yield startNewGame(chatId, nickname);
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
            case "btn_menu" -> {
                answerCallbackQuery(callBackQueryId);
                yield sendOrUpdateMenuMessage(chatId);
            }
            case "btn_menu_back" -> {
                answerCallbackQuery(callBackQueryId);
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
        var player = playersMap.get(chatId);
        val level = currentLevelsMap.get(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);
        if (!Room.Type.SHRINE.equals(currentRoom.getType())) {
            log.error("No shrine to use!");
            return false;
        }
        level.updateRoomType(point, Room.Type.SHRINE_DRAINED);
        player.refillHp();
        player.refillMana();
        playersMap.put(chatId, player);
        val messageId = sendOrUpdateRoomMessage(currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private boolean attack(Long chatId) {
        var player = playersMap.get(chatId);
        val level = currentLevelsMap.get(chatId);
        val point = player.getCurrentRoom();
        var currentRoom = level.getRoomByCoordinates(point);
        if (!Room.Type.MONSTER.equals(currentRoom.getType())) {
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
            level.updateRoomType(point, Room.Type.MONSTER_KILLED);
            currentRoom = level.getRoomByCoordinates(point);
            player.addXp(monster.getXpReward());
            monstersByChat.remove(chatId);
            val messageId = sendOrUpdateRoomMessage(currentRoom, player, chatId);
            if (messageId == -1) {
                return false;
            }
            lastMessageByChat.put(chatId, messageId);
            return true;
        } else {
            if (monster.getAttack() < player.getDefense()) {
                player.decreaseDefence(1);
            } else {
                player.decreaseHp(monster.getAttack() - player.getDefense());
                if (player.getHp() < 0) {
                    sendDeathMessage(chatId);
                    return true;
                }
            }
        }
        playersMap.put(chatId, player);
        val messageId = sendOrUpdateRoomMessage(currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private boolean collectTreasure(Long chatId) {
        var player = playersMap.get(chatId);
        val level = currentLevelsMap.get(chatId);
        val point = player.getCurrentRoom();
        var currentRoom = level.getRoomByCoordinates(point);
        if (!Room.Type.TREASURE.equals(currentRoom.getType())) {
            log.error("No treasure to collect!");
            return false;
        }
        player.addGold(((Treasure)currentRoom.getRoomContent()).getReward());
        level.updateRoomType(point, Room.Type.TREASURE_LOOTED);
        log.debug("Treasure looted!");
        currentRoom = level.getRoomByCoordinates(point);
        playersMap.put(chatId, player);
        val messageId = sendOrUpdateRoomMessage(currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private boolean moveBack(Long chatId) {
        var player = playersMap.get(chatId);
        val level = currentLevelsMap.get(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);
        val newDirection = getOppositeDirection(player.getDirection());
        val optionalRoom = currentRoom.getAdjacentRooms().get(newDirection);
        if (optionalRoom.isPresent()) {
            val nextRoom = level.getRoomByCoordinates(optionalRoom.get().getPoint());
            if (nextRoom == null) {
                log.error("Door on the back is locked!");
                return false;
            }
            player.setCurrentRoom(nextRoom.getPoint());
            player.setDirection(newDirection);
            if (Room.Type.MONSTER.equals(nextRoom.getType()) && !monstersByChat.containsKey(chatId)) {
                monstersByChat.put(chatId, (Monster) nextRoom.getRoomContent());
            }
            playersMap.put(chatId, player);
            level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
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
        var player = playersMap.get(chatId);
        val level = currentLevelsMap.get(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);
        val optionalRoom = currentRoom.getAdjacentRooms().get(player.getDirection());
        if (optionalRoom.isPresent()) {
            val nextRoom = level.getRoomByCoordinates(optionalRoom.get().getPoint());
            if (nextRoom == null) {
                log.error("Middle door is locked!");
                return false;
            }
            player.setCurrentRoom(nextRoom.getPoint());
            if (Room.Type.MONSTER.equals(nextRoom.getType()) && !monstersByChat.containsKey(chatId)) {
                monstersByChat.put(chatId, (Monster) nextRoom.getRoomContent());
            }
            playersMap.put(chatId, player);
            level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
            val messageId = sendOrUpdateRoomMessage(nextRoom, player, chatId);
            if (messageId == -1) {
                return false;
            }
            lastMessageByChat.put(chatId, messageId);
            log.debug("Moving to middle door: {}, updated direction: {}", nextRoom.getPoint(), player.getDirection());
            return true;
        } else {
            log.error("Middle door is locked!");
            return false;
        }
    }

    private boolean moveToRightRoom(Long chatId) {
        var player = playersMap.get(chatId);
        val level = currentLevelsMap.get(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);
        val newDirection = turnRight(player.getDirection());
        val optionalRoom = currentRoom.getAdjacentRooms().get(newDirection);
        if (optionalRoom.isPresent()) {
            val nextRoom = level.getRoomByCoordinates(optionalRoom.get().getPoint());
            if (nextRoom == null) {
                log.error("Right door is locked!");
                return false;
            }
            player.setCurrentRoom(nextRoom.getPoint());
            player.setDirection(newDirection);
            if (Room.Type.MONSTER.equals(nextRoom.getType()) && !monstersByChat.containsKey(chatId)) {
                monstersByChat.put(chatId, (Monster) nextRoom.getRoomContent());
            }
            playersMap.put(chatId, player);
            level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
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
        var player = playersMap.get(chatId);
        val level = currentLevelsMap.get(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);
        val newDirection = turnLeft(player.getDirection());
        val optionalRoom = currentRoom.getAdjacentRooms().get(newDirection);
        if (optionalRoom.isPresent()) {
            val nextRoom = level.getRoomByCoordinates(optionalRoom.get().getPoint());
            if (nextRoom == null) {
                log.error("Left door is locked!");
                return false;
            }
            player.setCurrentRoom(nextRoom.getPoint());
            player.setDirection(newDirection);
            if (Room.Type.MONSTER.equals(nextRoom.getType()) && !monstersByChat.containsKey(chatId)) {
                monstersByChat.put(chatId, (Monster) nextRoom.getRoomContent());
            }
            playersMap.put(chatId, player);
            level.getLevelMap().addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()]);
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

    private boolean startNewGame(Long chatId, String nickname) {
        final Level level = startLevel(chatId, 1);
        val direction = level.getStart().getAdjacentRooms().entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        val player = new Player(level.getStart().getPoint(), direction);
        playersMap.put(chatId, player);
        log.debug("Player generated: {}", player);
        val messageId = sendOrUpdateRoomMessage(level.getStart(), player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        log.debug("Player started level 1, current point, {}", level.getStart().getPoint());
        return true;
    }

    private boolean nextLevel(Long chatId) {
        val number = currentLevelsMap.get(chatId).getNumber() + 1;
        val level = startLevel(chatId, number);
        var player = playersMap.get(chatId);
        player.setCurrentRoom(level.getStart().getPoint());
        player.setDirection(level.getStart().getAdjacentRooms().entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null));
        val messageId = sendOrUpdateRoomMessage(level.getStart(), player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        log.debug("Player started level {}, current point, {}", number, level.getStart().getPoint());
        return true;
    }

    @NotNull
    private static Level startLevel(Long chatId, Integer levelNumber) {
        val level = new Level(levelNumber);
        currentLevelsMap.put(chatId, level);
        return level;
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
        sendMessage(message);
    }

    private boolean sendOrUpdateRoomMessage(Long chatId) {
        var player = playersMap.get(chatId);
        val level = currentLevelsMap.get(chatId);
        val point = player.getCurrentRoom();
        val currentRoom = level.getRoomByCoordinates(point);
        val messageId = sendOrUpdateRoomMessage(currentRoom, player, chatId);
        if (messageId == -1) {
            return false;
        }
        lastMessageByChat.put(chatId, messageId);
        return true;
    }

    private Integer sendOrUpdateRoomMessage(Room room, Player player, long chatId) {
        ClassPathResource imgFile = new ClassPathResource(getRoomAsset(room.getType()));
        String caption;
        if (monstersByChat.containsKey(chatId)) {
            caption = getRoomMessageCaption(player, monstersByChat.get(chatId));
        } else {
            caption = getRoomMessageCaption(player);
        }
        val keyboardMarkup = getRoomInlineKeyboardMarkup(room, player.getDirection());
        try (InputStream inputStream = imgFile.getInputStream()) {//TODO refactor using FileUtil
            if (lastMessageByChat.containsKey(chatId)) {
                val messageId = lastMessageByChat.get(chatId);

                val inputMedia = new InputMediaPhoto();
                inputMedia.setMedia(inputStream, imgFile.getFilename());
                inputMedia.setCaption(caption);

                val deleteMessage = DeleteMessage.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .build();
                try {
                    execute(deleteMessage);
                } catch (TelegramApiException e) {
                    log.error("Unable to edit message id:{}. {}", messageId, e);
                    return -1;
                }

            }
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

    private boolean sendOrUpdateMenuMessage(Long chatId) {
        val level = currentLevelsMap.get(chatId);
        val player = playersMap.get(chatId);
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
        if (messageContext.update().hasMessage()) {
            sendStartMessage(chatId);
        }
    }

    private void sendStartMessage(Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Welcome to dungeon!")
                .replyMarkup(getStartInlineKeyboardMarkup())
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
