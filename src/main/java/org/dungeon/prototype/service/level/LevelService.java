package org.dungeon.prototype.service.level;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.TurnUpdate;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.EmptyRoom;
import org.dungeon.prototype.model.room.content.HealthShrine;
import org.dungeon.prototype.model.room.content.ManaShrine;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.repository.LevelRepository;
import org.dungeon.prototype.repository.converters.mapstruct.LevelMapper;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.inventory.InventoryService;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.level.generation.LevelGenerationService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.nonNull;
import static org.dungeon.prototype.util.LevelUtil.getDirectionSwitchByCallBackData;
import static org.dungeon.prototype.util.LevelUtil.getErrorMessageByCallBackData;
import static org.dungeon.prototype.util.LevelUtil.getMonsterKilledRoomType;
import static org.dungeon.prototype.util.LevelUtil.getNextPointInDirection;
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
    @Autowired
    private ItemService itemService;
    @Autowired
    private InventoryService inventoryService;

    /**
     * Generates items and first level of new game
     * @param chatId id of chat where game starts
     * @return true if game successfully started
     */
    public boolean startNewGame(Long chatId) {
        itemService.generateItems(chatId);
        val defaultInventory = inventoryService.getDefaultInventory(chatId);
        var player = playerService.getPlayerPreparedForNewGame(chatId, defaultInventory);
        player = effectService.updatePlayerEffects(player);
        player = effectService.updateArmorEffect(player);
        log.debug("Player loaded: {}", player);
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
    public boolean nextLevel(Long chatId) {
        var player = playerService.getPlayer(chatId);
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
    public boolean continueGame(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val levelNumber = getLevelNumber(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (Objects.isNull(currentRoom)) {
            log.error("Couldn't find current room by id: {}", player.getCurrentRoomId());
            return false;
        }
        log.debug("Player continued level {}, current point: {}", levelNumber, player.getCurrentRoom());
        return messageService.sendRoomMessage(chatId, player, currentRoom);
    }

    /**
     * Processes moving to next room and sending corresponding room message
     * @param chatId id of chat where game runs
     * @param callBackData defines movement direction, values:
     *                     {@link CallbackType.LEFT}, {@link CallbackType.RIGHT},
     *                     {@link CallbackType.FORWARD}, {@link CallbackType.BACK}
     * @return true if player successfully moved to next room
     */
    @TurnUpdate
    public boolean moveToRoom(Long chatId, CallbackType callBackData) {
        var player = playerService.getPlayer(chatId);
        var level = getLevel(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (Objects.isNull(currentRoom)) {
            log.error("Unable to find room with id {}", player.getCurrentRoomId());
            return false;
        }
        val newDirection = getDirectionSwitchByCallBackData(player.getDirection(), callBackData);
        val errorMessage = getErrorMessageByCallBackData(callBackData);
        if (!currentRoom.getAdjacentRooms().containsKey(newDirection) || !currentRoom.getAdjacentRooms().get((newDirection))) {
            log.error(errorMessage);
            return false;
        }
        val nextRoom = level.getRoomByCoordinates(getNextPointInDirection(currentRoom.getPoint(), newDirection));
        if (nextRoom == null) {
            log.error(errorMessage);
            return false;
        }
        val levelMap = level.getLevelMap();
        if (levelMap.isContainsRoom(nextRoom.getPoint().getX(), nextRoom.getPoint().getY()) ||
                levelMap.addRoom(level.getGrid()[nextRoom.getPoint().getX()][nextRoom.getPoint().getY()])) {
            player.setCurrentRoom(nextRoom.getPoint());
            player.setCurrentRoomId(nextRoom.getId());
            player.setDirection(newDirection);
            player = playerService.updatePlayer(player);
            level = saveOrUpdateLevel(level);
            if (nonNull(player) && nonNull(level)) {
                log.debug("Moving to {} door: {}, updated direction: {}", callBackData.toString().toLowerCase(), nextRoom.getPoint(), player.getDirection());
                return messageService.sendRoomMessage(chatId, player, nextRoom);
            }
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

    public boolean shrineRefill(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val level = getLevel(chatId);
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
        if (currentRoom.getRoomContent().getRoomType().equals(RoomType.HEALTH_SHRINE)) {
            player.addEffects(List.of(((HealthShrine) currentRoom.getRoomContent()).getEffect()));
        }
        if (currentRoom.getRoomContent().getRoomType().equals(RoomType.MANA_SHRINE)) {
            player.addEffects(List.of(((ManaShrine) currentRoom.getRoomContent()).getEffect()));
        }
        level.updateRoomType(currentRoom.getPoint(), RoomType.SHRINE_DRAINED);
        playerService.updatePlayer(player);
        return messageService.sendRoomMessage(chatId, player, currentRoom);
    }

    public Level saveOrUpdateLevel(Level level) {
        val levelDocument = LevelMapper.INSTANCE.mapToDocument(level);
        val savedLevelDocument = levelRepository.save(levelDocument);
        return LevelMapper.INSTANCE.mapToLevel(savedLevelDocument);
    }

    public boolean sendOrUpdateMapMessage(Long chatId) {
        val player = playerService.getPlayer(chatId);
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
        //TODO: adjust query
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
}
