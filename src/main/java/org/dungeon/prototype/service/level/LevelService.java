package org.dungeon.prototype.service.level;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.annotations.aspect.ChatStateUpdate;
import org.dungeon.prototype.annotations.aspect.RoomInitialization;
import org.dungeon.prototype.annotations.aspect.TurnUpdate;
import org.dungeon.prototype.bot.state.ChatState;
import org.dungeon.prototype.exception.EntityNotFoundException;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.level.Level;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.EmptyRoom;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.model.room.content.Shrine;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.repository.mongo.LevelRepository;
import org.dungeon.prototype.repository.mongo.converters.mapstruct.LevelMapper;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.level.generation.LevelGenerationService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.dungeon.prototype.bot.state.ChatState.GAME;
import static org.dungeon.prototype.util.LevelUtil.getMonsterKilledRoomType;
import static org.dungeon.prototype.util.LevelUtil.printMap;

@Slf4j
@Component
public class LevelService {
    @Autowired
    LevelGenerationService levelGenerationService;
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private RoomService roomService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private EffectService effectService;

    /**
     * Generates items and first level of new game
     *
     * @param chatId id of chat where game starts
     */
    public void startNewGame(Long chatId, Player player) {
        Level level = startNewLevel(chatId, player, 1);
        log.info("Starting new game...");
        messageService.sendNewLevelMessage(chatId, player, level, 1);
    }

    /**
     * Generates next level of game and starts it
     *
     * @param chatId id of chat where game runs
     */
    public void nextLevel(Long chatId, Player player) {
        val number = getLevelNumber(chatId) + 1;
        val level = startNewLevel(chatId, player, number);
        player = effectService.updateArmorEffect(player);
        player.restoreArmor();
        playerService.updatePlayer(player);
        messageService.sendNewLevelMessage(chatId, player, level, number);
    }

    /**
     * Continues saved game, loads last visited room
     *
     * @param chatId id of chat for which game saved
     */
    @RoomInitialization
    @ChatStateUpdate(from = ChatState.PRE_GAME_MENU, to = GAME)
    public void continueGame(Long chatId, Player player, Room currentRoom) {
        val levelNumber = getLevelNumber(chatId);
        log.info("Player continued level {}, current point: {}", levelNumber, player.getCurrentRoom());
        if (currentRoom.getRoomContent() instanceof MonsterRoom) {
            messageService.sendMonsterRoomMessage(chatId, player, currentRoom);
        } else {
            messageService.sendRoomMessage(chatId, player, currentRoom);
        }
    }

    /**
     * Processes moving to next room and sending corresponding room message
     *
     * @param chatId       id of chat where game runs
     * @param player       current player
     * @param nextRoom     room player moves to
     * @param newDirection direction of movement
     */
    @TurnUpdate
    @RoomInitialization
    public void moveToRoom(Long chatId, Player player, Room nextRoom, Direction newDirection) {
        var level = getLevel(chatId);
        val levelMap = level.getLevelMap();
        if (levelMap.isContainsRoom(nextRoom.getPoint().getX(), nextRoom.getPoint().getY()) ||
                levelMap.addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()])) {
            saveOrUpdateLevel(level);
            player.setCurrentRoom(nextRoom.getPoint());
            player.setCurrentRoomId(nextRoom.getId());
            player.setDirection(newDirection);
            playerService.updatePlayer(player);
            if (nextRoom.getRoomContent() instanceof MonsterRoom) {
                messageService.sendMonsterRoomMessage(chatId, player, nextRoom);
            } else {
                messageService.sendRoomMessage(chatId, player, nextRoom);
            }
        }
    }

    /**
     * Performs required updates after player kills monster
     *
     * @param currentRoom current room for updating {@link RoomContent}
     */
    public void updateAfterMonsterKill(Room currentRoom) {
        val roomType = getMonsterKilledRoomType(currentRoom.getRoomContent().getRoomType());
        currentRoom.setRoomContent(new EmptyRoom(roomType));
        roomService.saveOrUpdateRoomContent(currentRoom.getRoomContent());
        roomService.saveOrUpdateRoom(currentRoom);
        val level = getLevel(currentRoom.getChatId());
        level.updateRoomType(currentRoom.getPoint(), roomType);
        saveOrUpdateLevel(level);
    }

    /**
     * Performs required updates after player empties out treasure
     *
     * @param currentRoom current room for updating {@link RoomContent}
     */
    public void updateAfterTreasureLooted(Room currentRoom) {
        currentRoom.setRoomContent(new EmptyRoom(RoomType.TREASURE_LOOTED));
        roomService.saveOrUpdateRoomContent(currentRoom.getRoomContent());
        roomService.saveOrUpdateRoom(currentRoom);
        val level = getLevel(currentRoom.getChatId());
        level.updateRoomType(currentRoom.getPoint(), RoomType.TREASURE_LOOTED);
        saveOrUpdateLevel(level);
    }

    /**
     * Adds regeneration effect stored in shrine to player, making shrine unusable
     *
     * @param chatId      current chat id
     * @param player      current player
     * @param currentRoom shrine room
     * @param level       current level
     */
    public void shrineUsage(Long chatId, Player player, Room currentRoom, Level level) {
        if (currentRoom.getRoomContent() instanceof Shrine) {
            player.addEffect(((Shrine) currentRoom.getRoomContent()).getEffect());
            level.updateRoomType(currentRoom.getPoint(), RoomType.SHRINE_DRAINED);
            saveOrUpdateLevel(level);
            currentRoom.setRoomContent(new EmptyRoom(RoomType.SHRINE_DRAINED));
            roomService.saveOrUpdateRoomContent(currentRoom.getRoomContent());
            roomService.saveOrUpdateRoom(currentRoom);
            playerService.updatePlayer(player);
            messageService.sendRoomMessage(chatId, player, currentRoom);
        }
    }

    /**
     * Saves level to repository
     *
     * @param level level to save or update
     * @return saved or updated level
     */
    public Level saveOrUpdateLevel(Level level) {
        level.setRoomsMap(level.getRoomsMap().entrySet().stream()
                .peek(entry -> {
                    if (isNull(entry.getValue().getId())) {
                        entry.setValue(roomService.saveOrUpdateRoom(entry.getValue()));
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        val levelDocument = LevelMapper.INSTANCE.mapToDocument(level);
        val savedLevelDocument = levelRepository.save(levelDocument);
        return LevelMapper.INSTANCE.mapToLevel(savedLevelDocument);
    }

    /**
     * Sends map message to given player's chat
     *
     * @param chatId current chat id
     * @param player current player
     */
    public void sendMapMessage(Long chatId, Player player) {
        val level = getLevel(chatId);
        messageService.sendMapMenuMessage(chatId,
                printMap(level.getGrid(), level.getLevelMap(), player.getCurrentRoom(), player.getDirection()));
    }

    /**
     * Looks for current level of given chat
     * throws {@link EntityNotFoundException} if none found
     *
     * @param chatId current chat id
     * @return found level
     */
    public Level getLevel(Long chatId) {
        val levelDocument = levelRepository.findByChatId(chatId).orElseThrow(() ->
                new EntityNotFoundException(chatId, "level", CallbackType.MENU_BACK));
        return LevelMapper.INSTANCE.mapToLevel(levelDocument);
    }

    /**
     * Removes level by chat id
     *
     * @param chatId current chat id
     */
    public void remove(Long chatId) {
        levelRepository.removeByChatId(chatId);
    }

    private Integer getLevelNumber(Long chatId) {
        val projection = levelRepository.findNumberByChatId(chatId)
                .orElseThrow(() ->
                        new EntityNotFoundException(chatId, "level number", CallbackType.MENU_BACK));
        return projection.getNumber();
    }

    public Level startNewLevel(Long chatId, Player player, Integer levelNumber) {
        messageService.sendLevelGeneratingInfoMessage(chatId, levelNumber);
        var level = levelGenerationService.generateAndPopulateLevel(chatId, player, levelNumber);
        if (levelRepository.existsByChatId(chatId)) {
            levelRepository.removeByChatId(chatId);
        }
        level = saveOrUpdateLevel(level);
        log.info("Level generated: {}", level);
        val direction = level.getRoomsMap().get(level.getStart()).getAdjacentRooms().entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()) && entry.getValue())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        player.setDirection(direction);
        player.setCurrentRoom(level.getStart());
        player.setCurrentRoomId(level.getRoomsMap().get(level.getStart()).getId());
        playerService.updatePlayer(player);
        return level;
    }

    public boolean hasLevel(Long chatId) {
        return levelRepository.existsByChatId(chatId);
    }

    public Room getRoomByChatIdAndCoordinates(Long chatId, Point point) {
        val room = getLevel(chatId).getRoomByCoordinates(point);
        if (isNull(room)) {
            throw new EntityNotFoundException(chatId, "room", CallbackType.MENU_BACK,
                    Pair.create("point", point.toString()));
        } else {
            return room;
        }
    }
}
