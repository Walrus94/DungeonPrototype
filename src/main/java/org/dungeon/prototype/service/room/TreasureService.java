package org.dungeon.prototype.service.room;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.inventory.InventoryService;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.message.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class TreasureService {

    @Autowired
    PlayerService playerService;
    @Autowired
    LevelService levelService;
    @Autowired
    RoomService roomService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    MessageService messageService;

    /**
     * Opens treasure if present
     * @param chatId current chat id
     * @param player current player
     */
    public void openTreasure(Long chatId, Player player) {
        val treasure = openTreasure(chatId, player.getCurrentRoomId());
        if (treasure.isPresent()) {
            messageService.sendTreasureMessage(chatId, treasure.get());
        } else {
            val room = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
            messageService.sendRoomMessage(chatId, player, room);
        }
    }

    /**
     * Collects gold from treasure
     * @param chatId current chat id
     * @param player current player
     * @param currentRoom treasure room
     */
    public void collectTreasureGold(Long chatId, Player player, Room currentRoom) {
        val treasure = (Treasure) currentRoom.getRoomContent();

        player.addGold(treasure.getGold());
        treasure.setGold(0);
        playerService.updatePlayer(player);
        if (treasure.getGold() == 0 && treasure.getItems().isEmpty()) {
            levelService.updateAfterTreasureLooted(currentRoom);
            messageService.sendRoomMessage(chatId, player, currentRoom);
        } else {
            roomService.saveOrUpdateRoom(currentRoom);
            messageService.sendTreasureMessage(chatId, treasure);
        }
    }

    public void collectAllTreasure(Long chatId, Player player, Room currentRoom) {
        val treasure = (Treasure) currentRoom.getRoomContent();
        log.debug("Treasure contents - gold: {}, items: {}", treasure.getGold(), treasure.getItems());
        val items = treasure.getItems();
        val gold = treasure.getGold();

        player.addGold(gold);
        treasure.setGold(0);
        if (!items.isEmpty()) {
            if (!player.getInventory().addItems(items)) {
                log.info("No room in the inventory!");
                playerService.updatePlayer(player);
                roomService.saveOrUpdateRoom(currentRoom);
                return;
            } else {
                treasure.setItems(Collections.emptySet());
            }
        }
        playerService.updatePlayer(player);
        levelService.updateAfterTreasureLooted(currentRoom);
        inventoryService.saveOrUpdateInventory(player.getInventory());
        messageService.sendRoomMessage(chatId, player, currentRoom);
    }

    public boolean collectTreasureItem(Long chatId, String itemId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
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
            if (treasure.getGold() == 0 && treasure.getItems().isEmpty()) {
                levelService.updateAfterTreasureLooted(currentRoom);
            } else {
                roomService.saveOrUpdateRoom(currentRoom);
            }
            playerService.updatePlayer(player);
            inventoryService.saveOrUpdateInventory(player.getInventory());
        } else {
            log.info("No room in inventory!");
            return false;
        }
        messageService.sendTreasureMessage(chatId, treasure);
        return true;
    }

    private Optional<Treasure> openTreasure(Long chatId, String roomId) {
        val room = roomService.getRoomByIdAndChatId(chatId, roomId);
        if (!RoomType.TREASURE.equals(room.getRoomContent().getRoomType())) {
            log.error("No treasure to collect!");
            return Optional.empty();
        }

        val treasure = (Treasure) room.getRoomContent();
        if (treasure.getGold() == 0 && treasure.getItems().isEmpty()) {
            log.debug("Treasure looted!");
            levelService.updateAfterTreasureLooted(room);
            return Optional.empty();
        }
        return Optional.of(treasure);
    }
}
