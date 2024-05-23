package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Player;
import org.dungeon.prototype.model.Room;
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
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.dungeon.prototype.util.FileUtil.getRoomAsset;
import static org.dungeon.prototype.util.KeyboardUtil.getRoomInlineKeyboardMarkup;
import static org.dungeon.prototype.util.KeyboardUtil.getStartInlineKeyboardMarkup;
import static org.dungeon.prototype.util.LevelUtil.Direction;
import static org.dungeon.prototype.util.LevelUtil.getOppositeDirection;
import static org.dungeon.prototype.util.LevelUtil.printMap;
import static org.dungeon.prototype.util.LevelUtil.turnLeft;
import static org.dungeon.prototype.util.LevelUtil.turnRight;

@Slf4j
@Component
public class DungeonBot extends AbilityBot {
    private static Map<Long, Level> currentLevelsMap;
    @Autowired
    public DungeonBot(@Value("${bot.token}") String botToken, @Value("${bot.username}") String botUsername) {
        super(botToken, botUsername);
        playersMap = db.getMap("players");
        currentLevelsMap = new HashMap<>();
    }

    @Override
    public long creatorId() {
        return 151557417L;
    }
    private final Map<Long, Player> playersMap;

    public Ability start() {
        return Ability.builder()
                .name("start")
                .info("Starts bot")
                .locality(Locality.USER)
                .privacy(Privacy.PUBLIC)
                .action(this::processStartAction)
                .build();
    }

    public Ability showMap() {
        return Ability.builder()
                .name("map")
                .info("Shows map")
                .locality(Locality.USER)
                .privacy(Privacy.PUBLIC)
                .action(this::displayMap)
                .build();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            if (!handleCallbackQuery(update.getCallbackQuery())) {
                super.onUpdateReceived(update);
            }
        } else {
            super.onUpdateReceived(update);
        }
    }

    private boolean handleCallbackQuery(CallbackQuery callbackQuery) {
        val callBackQueryId = callbackQuery.getId();
        val callData = callbackQuery.getData();
        val nickname = callbackQuery.getFrom().getUserName() == null ?
                callbackQuery.getFrom().getFirstName() :
                "@" +callbackQuery.getFrom().getUserName();
        val chatId = callbackQuery.getMessage().getChatId();
        return switch (callData) {
            case "start_game" -> {
                answerCallbackQuery(callBackQueryId);
                yield startNewGame(chatId);
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
            playersMap.put(chatId, player);
            sendRoomMessage(nextRoom, newDirection, chatId);
            log.debug("Moving back to {}, updated direction: {}", nextRoom.getPoint(), player.getDirection());
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
            playersMap.put(chatId, player);
            sendRoomMessage(nextRoom, player.getDirection(), chatId);
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
            playersMap.put(chatId, player);
            sendRoomMessage(nextRoom, newDirection, chatId);
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
            playersMap.put(chatId, player);
            sendRoomMessage(nextRoom, newDirection, chatId);
            log.debug("Moving to left door: {}, updated direction: {}", nextRoom.getPoint(), player.getDirection());
            return true;
        } else {
            log.error("Left door is locked!");
            return false;
        }
    }

    private boolean startNewGame(Long chatId) {
        val level = new Level(1);
        currentLevelsMap.put(chatId, level);
        val direction = level.getStart().getAdjacentRooms().entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        val player = new Player(level.getStart().getPoint(), direction, 0L, 20, 10);
        playersMap.put(chatId, player);
        log.debug("Player generated: {}", player);
        sendRoomMessage(level.getStart(), direction, chatId);
        log.debug("Player started level, current point, {}", level.getStart().getPoint());
        return true;
    }

    private void displayMap(MessageContext messageContext) {
        val level = currentLevelsMap.get(messageContext.chatId());
        val levelMap = printMap(level.getGrid());
        sendMapMessage(messageContext.chatId(), levelMap);
    }

    private void sendRoomMessage(Room room, Direction direction, long chatId) {
        ClassPathResource imgFile = new ClassPathResource(getRoomAsset(room));
        try (InputStream inputStream = imgFile.getInputStream()) {//TODO refactor using FileUtil
            var message = SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(inputStream, imgFile.getFilename()))
                    .replyMarkup(getRoomInlineKeyboardMarkup(room, direction))
                    .build();
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Unable to send message: ", e);
            }
        } catch (IOException e) {
            log.error("Error loading file: {}", e.getMessage());
        }
    }

    private void sendMapMessage(Long chatId, String levelMap) {
        val message = SendMessage.builder()
                .chatId(chatId)
                .text(levelMap)
                .build();
        sendMessage(message);
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
        sendMessage(message);
    }

    private void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Unable to send message: ", e);
        }
    }
}
