package org.dungeon.prototype.service.room;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.SendMerchantBuyItem;
import org.dungeon.prototype.annotations.aspect.SendMerchantBuyMenuMessage;
import org.dungeon.prototype.annotations.aspect.SendMerchantSellMenuMessage;
import org.dungeon.prototype.annotations.aspect.SendTreasureMessage;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.repository.RoomContentRepository;
import org.dungeon.prototype.repository.RoomRepository;
import org.dungeon.prototype.repository.converters.mapstruct.RoomContentMapper;
import org.dungeon.prototype.repository.converters.mapstruct.RoomMapper;
import org.dungeon.prototype.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Component
public class RoomService {

    @Autowired
    PlayerService playerService;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    RoomContentRepository roomContentRepository;

    public Room getRoomByIdAndChatId(Long chatId, String id) {
        val roomDocument = roomRepository.findByChatIdAndId(chatId, id);
        return RoomMapper.INSTANCE.mapToRoom(roomDocument);
    }

    public Room saveOrUpdateRoom(Room room) {
        val roomDocument = RoomMapper.INSTANCE.mapToDocument(room);
        if (nonNull(roomDocument.getRoomContent()) && isNull(roomDocument.getRoomContent().getId())) {
            roomContentRepository.save(roomDocument.getRoomContent());
        }
        val savedRoomDocument = roomRepository.save(roomDocument);
        return RoomMapper.INSTANCE.mapToRoom(savedRoomDocument);
    }

    public RoomContent saveOrUpdateRoomContent(RoomContent roomContent) {
        val roomContentDocument = RoomContentMapper.INSTANCE.mapToRoomContentDocument(roomContent);
        val savedRoomContentDocument = roomContentRepository.save(roomContentDocument);
        return RoomContentMapper.INSTANCE.mapToRoomContent(savedRoomContentDocument);
    }

    @SendTreasureMessage
    public boolean openTreasure(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val treasure = openTreasure(chatId, player.getCurrentRoomId());
        return treasure.isPresent();
    }

    @SendTreasureMessage
    public boolean collectTreasureGold(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        val treasure = (Treasure) currentRoom.getRoomContent();

        player.addGold(treasure.getGold());
        treasure.setGold(0);
        saveOrUpdateRoom(currentRoom);
        playerService.updatePlayer(player);
        return true;
    }

    @SendMerchantBuyMenuMessage
    public boolean openMerchantBuyMenu(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        return currentRoom.getRoomContent() instanceof Merchant;
    }

    @SendMerchantSellMenuMessage
    public boolean openMerchantSellMenu(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        return currentRoom.getRoomContent() instanceof Merchant;
    }

    @SendMerchantBuyItem
    public boolean openMerchantBuyItem(Long chatId, String itemId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (currentRoom.getRoomContent() instanceof Merchant merchant) {
            return merchant.getItems().stream().anyMatch(item -> itemId.equals(item.getId()));
        } else {
            return false;
        }
    }

    private Optional<Treasure> openTreasure(Long chatId, String roomId) {
        val room = getRoomByIdAndChatId(chatId, roomId);
        if (!RoomType.TREASURE.equals(room.getRoomContent().getRoomType())) {
            log.error("No treasure to collect!");
            return Optional.empty();
        }

        val treasure = (Treasure) room.getRoomContent();
        if (treasure.getGold() == 0 && treasure.getItems().isEmpty()) {
            log.debug("Treasure looted!");
            saveOrUpdateRoom(room);
            return Optional.empty();
        }
        return Optional.of(treasure);
    }

}
