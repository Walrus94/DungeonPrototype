package org.dungeon.prototype.service.level;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.RoomInitialization;
import org.dungeon.prototype.annotations.aspect.TurnUpdate;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.EmptyRoom;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.model.room.content.Shrine;
import org.dungeon.prototype.repository.LevelRepository;
import org.dungeon.prototype.repository.converters.mapstruct.LevelMapper;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.level.generation.LevelGenerationService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

import static java.util.Objects.nonNull;
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
     * @param chatId id of chat where game starts
     * @return true if game successfully started
     */
    public boolean startNewGame(Long chatId, Player player) {
        val level = startNewLevel(chatId, player, 1);
        if (nonNull(level)) {
            log.debug("Player started level 1, current point: {}\nPlayer: {}", level.getStart().getPoint(), player);
            return messageService.sendRoomMessage(chatId, player, level.getStart());
        } else {
            return false;
        }
    }

    /**
     * Generates next level of game and starts it
     * @param chatId id of chat where game runs
     * @return true if playe successfully progressed to next level
     */
    public boolean nextLevel(Long chatId, Player player) {
        val number = getLevelNumber(chatId) + 1;
        val level = startNewLevel(chatId, player, number);
        player = effectService.updateArmorEffect(player);
        player.restoreArmor();
        playerService.updatePlayer(player);
        log.debug("Player started level {}, current point, {}", number, level.getStart().getPoint());
        return messageService.sendRoomMessage(chatId, player, level.getStart());
    }

    /**
     * Continues saved game, loads last visited room
     * @param chatId id of chat for which game saved
     * @return true if game successfully continued
     */
    @RoomInitialization
    public boolean continueGame(Long chatId, Player player, Room currentRoom) {
        val levelNumber = getLevelNumber(chatId);
        log.debug("Player continued level {}, current point: {}", levelNumber, player.getCurrentRoom());
        return messageService.sendRoomMessage(chatId, player, currentRoom);
    }

    /**
     * Processes moving to next room and sending corresponding room message
     * @param chatId id of chat where game runs
     * @param player current player
     * @param nextRoom room player moves to
     * @param newDirection direction of movement
     * @return true if player successfully moved to next room
     */
    @TurnUpdate
    @RoomInitialization
    public boolean moveToRoom(Long chatId, Player player, Room nextRoom, Direction newDirection) {
        var level = getLevel(chatId);
        val levelMap = level.getLevelMap();
        if (levelMap.isContainsRoom(nextRoom.getPoint().getX(), nextRoom.getPoint().getY()) ||
                levelMap.addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()])) {
            saveOrUpdateLevel(level);
            player.setCurrentRoom(nextRoom.getPoint());
            player.setCurrentRoomId(nextRoom.getId());
            player.setDirection(newDirection);
            playerService.updatePlayer(player);
            return messageService.sendRoomMessage(chatId, player, nextRoom);
        }
        return false;
    }

    /**
     * Performs required updates after player kills monster
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
     * @param currentRoom current room for updating {@link RoomContent}
     */
    public void updateAfterTreasureLooted(Room currentRoom) {
        currentRoom.setRoomContent(new EmptyRoom(RoomType.TREASURE_LOOTED));
        roomService.saveOrUpdateRoom(currentRoom);
        val level = getLevel(currentRoom.getChatId());
        level.updateRoomType(currentRoom.getPoint(), RoomType.TREASURE_LOOTED);
        saveOrUpdateLevel(level);
    }

    public boolean shrineRefill(Long chatId, Player player, Room currentRoom, Level level) {
        if (!RoomType.HEALTH_SHRINE.equals(currentRoom.getRoomContent().getRoomType()) &&
                !RoomType.MANA_SHRINE.equals(currentRoom.getRoomContent().getRoomType())) {
            log.error("No shrine to use!");
            return false;
        }
        player.addEffect(((Shrine) currentRoom.getRoomContent()).getEffect());
        level.updateRoomType(currentRoom.getPoint(), RoomType.SHRINE_DRAINED);
        playerService.updatePlayer(player);
        return messageService.sendRoomMessage(chatId, player, currentRoom);
    }

    public Level saveOrUpdateLevel(Level level) {
        val levelDocument = LevelMapper.INSTANCE.mapToDocument(level);
        val savedLevelDocument = levelRepository.save(levelDocument);
        return LevelMapper.INSTANCE.mapToLevel(savedLevelDocument);
    }

    public boolean sendOrUpdateMapMessage(Long chatId, Player player) {
        val level = getLevel(chatId);
        messageService.sendMapMenuMessage(chatId,
                printMap(level.getGrid(), level.getLevelMap(), player.getCurrentRoom(), player.getDirection()));
        return true;
    }

    public Level getLevel(Long chatId) {
        val levelDocument =  levelRepository.findByChatId(chatId).orElseGet(() -> {
            log.error("Unable to find level by chatId: {}", chatId);
            return null;
        });
        return LevelMapper.INSTANCE.mapToLevel(levelDocument);
    }

    public void remove(Long chatId) {
        levelRepository.removeByChatId(chatId);
    }

    private Integer getLevelNumber(Long chatId) {
        val projection = levelRepository.findNumberByChatId(chatId).orElseGet(() -> {
            log.error("Unable to fetch level number by chatId");
            return null;
        });
        return projection.getNumber();
    }

    private Level startNewLevel(Long chatId, Player player, Integer levelNumber) {
        var level = levelGenerationService.generateLevel(chatId, player, levelNumber);
        val direction = level.getStart().getAdjacentRooms().entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()) && entry.getValue())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        player.setDirection(direction);
        player.setCurrentRoom(level.getStart().getPoint());
        player.setCurrentRoomId(level.getStart().getId());
        if (levelRepository.existsByChatId(chatId)) {
            levelRepository.removeByChatId(chatId);
        }
        playerService.updatePlayer(player);
        return saveOrUpdateLevel(level);
    }

    public boolean hasLevel(Long chatId) {
        return levelRepository.existsByChatId(chatId);
    }

    public Room getRoomByChatIdAndCoordinates(Long chatId, Point point) {
        return getLevel(chatId).getRoomByCoordinates(point);
    }
}
