package org.dungeon.prototype.service.room;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.exception.EntityNotFoundException;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.repository.RoomContentRepository;
import org.dungeon.prototype.repository.RoomRepository;
import org.dungeon.prototype.repository.converters.mapstruct.RoomContentMapper;
import org.dungeon.prototype.repository.converters.mapstruct.RoomMapper;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.message.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    @Autowired
    EffectService effectService;
    @Autowired
    MessageService messageService;

    /**
     * Collects required data and passes it to {@link MessageService}
     * to build and send room message
     *
     * @param chatId id of chat where message sent
     *               ]
     */
    public void sendOrUpdateRoomMessage(Long chatId, Player player) {
        val room = getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        messageService.sendRoomMessage(chatId, player, room);
    }

    /**
     * Looks for room in repository by passed parameters
     *
     * @param chatId id of chat
     * @param id     of requested room
     * @return room
     */
    public Room getRoomByIdAndChatId(Long chatId, String id) {
        val roomDocument = roomRepository.findByChatIdAndId(chatId, id).orElseThrow(() ->
                    new EntityNotFoundException(chatId, "room", CallbackType.DEFAULT_ERROR_RETURN,
                            Pair.create("roomId", id)));
        return RoomMapper.INSTANCE.mapToRoom(roomDocument);
    }

    /**
     * Saves room to.../Updates room in
     * corresponding repository
     *
     * @param room room to save or update
     * @return saved or updated room
     */
    public Room saveOrUpdateRoom(Room room) {
        val roomDocument = RoomMapper.INSTANCE.mapToDocument(room);
        if (nonNull(roomDocument.getRoomContent()) && isNull(roomDocument.getRoomContent().getId())) {
            roomContentRepository.save(roomDocument.getRoomContent());
        }
        val savedRoomDocument = roomRepository.save(roomDocument);
        return RoomMapper.INSTANCE.mapToRoom(savedRoomDocument);
    }

    /**
     * Saves room content to.../Updates room content in
     * corresponding repository
     *
     * @param roomContent room content to save or update
     * @return saved or updated room content
     */
    public RoomContent saveOrUpdateRoomContent(RoomContent roomContent) {
        val roomContentDocument = RoomContentMapper.INSTANCE.mapToRoomContentDocument(roomContent);
        val savedRoomContentDocument = roomContentRepository.save(roomContentDocument);
        return RoomContentMapper.INSTANCE.mapToRoomContent(savedRoomContentDocument);
    }

    /**
     * Sends menu with list of merchant's items to buy
     *
     * @param chatId      id of chat where message sent
     * @param currentRoom current room
     */
    public void openMerchantBuyMenu(Long chatId, Player player, Room currentRoom) {
        if (currentRoom.getRoomContent() instanceof Merchant merchant) {
            messageService.sendMerchantBuyMenuMessage(chatId, player.getGold(), merchant.getItems());
        }
    }

    /**
     * Sends menu with list of player's items to sell
     *
     * @param chatId id of chat where message sent
     */
    public void openMerchantSellMenu(Long chatId, Player player, Room currentRoom) {
        if (currentRoom.getRoomContent() instanceof Merchant) {
            messageService.sendMerchantSellMenuMessage(chatId, player);
        }
    }

    /**
     * Opens description menu of one of the merchant's items
     *
     * @param chatId id of chat where message sent
     * @param itemId id of merchant's item
     */
    public void openMerchantBuyItem(Long chatId, String itemId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (currentRoom.getRoomContent() instanceof Merchant merchant &&
                merchant.getItems().stream().anyMatch(item -> itemId.equals(item.getId()))) {
            messageService.sendMerchantBuyItemMessage(chatId,
                    merchant.getItems().stream()
                            .filter(item -> itemId.equals(item.getId()))
                            .findFirst().orElseThrow(() ->
                                    new EntityNotFoundException(chatId, "item", CallbackType.DEFAULT_ERROR_RETURN,
                                            Pair.create("itemId", itemId))));
        }
    }

    /**
     * Restore player's armor
     *
     * @param chatId id of player's chat
     */
    public void restoreArmor(Long chatId) {
        var player = playerService.getPlayer(chatId);
        player = effectService.updateArmorEffect(player);
        player.restoreArmor();
        playerService.updatePlayer(player);
        val currentRoom = getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        messageService.sendRoomMessage(chatId, player, currentRoom);
    }

    /**
     * Adds 1 to given player's attribute
     *
     * @param chatId          id of player's chat
     * @param playerAttribute attribute to upgrade
     */
    public void upgradePlayerAttribute(Long chatId, PlayerAttribute playerAttribute) {
        val player = playerService.getPlayer(chatId);
        player.getAttributes().put(playerAttribute, player.getAttributes().get(playerAttribute) + 1);
        playerService.updatePlayer(player);
        val currentRoom = getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        messageService.sendRoomMessage(chatId, player, currentRoom);
    }

}
