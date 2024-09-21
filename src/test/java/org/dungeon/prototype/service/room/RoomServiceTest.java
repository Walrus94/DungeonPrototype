package org.dungeon.prototype.service.room;

import lombok.val;
import org.dungeon.prototype.model.document.item.ItemDocument;
import org.dungeon.prototype.model.document.room.RoomContentDocument;
import org.dungeon.prototype.model.document.room.RoomDocument;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.EmptyRoom;
import org.dungeon.prototype.repository.RoomContentRepository;
import org.dungeon.prototype.repository.RoomRepository;
import org.dungeon.prototype.service.BaseServiceUnitTest;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.message.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Set;
import java.util.stream.Collectors;

import static org.dungeon.prototype.TestData.getMerchant;
import static org.dungeon.prototype.TestData.getPlayer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomServiceTest extends BaseServiceUnitTest {

    @InjectMocks
    RoomService roomService;
    @Mock
    PlayerService playerService;
    @Mock
    RoomRepository roomRepository;
    @Mock
    RoomContentRepository roomContentRepository;
    @Mock
    EffectService effectService;
    @Mock
    MessageService messageService;

    @Test
    @DisplayName("Successfully sends current room message")
    void sendOrUpdateRoomMessage() {
        val player = getPlayer(CHAT_ID);
        val roomDocument = new RoomDocument();
        roomDocument.setChatId(CHAT_ID);
        roomDocument.setId(CURRENT_ROOM_ID);

        when(roomRepository.findByChatIdAndId(CHAT_ID, player.getCurrentRoomId())).thenReturn(roomDocument);
        ArgumentCaptor<Room> roomArgumentCaptor = ArgumentCaptor.forClass(Room.class);
        when(messageService.sendRoomMessage(eq(CHAT_ID), eq(player), roomArgumentCaptor.capture())).thenReturn(true);

        val actualResult = roomService.sendOrUpdateRoomMessage(CHAT_ID, player);

        assertTrue(actualResult);
        val actualRoom = roomArgumentCaptor.getValue();
        assertEquals(roomDocument.getChatId(), actualRoom.getChatId());
        assertEquals(roomDocument.getId(), actualRoom.getId());
    }

    @Test
    @DisplayName("Successfully loads room from repository")
    void getRoomByIdAndChatId() {
        val roomDocument = new RoomDocument();
        roomDocument.setId(CURRENT_ROOM_ID);
        roomDocument.setChatId(CHAT_ID);
        when(roomRepository.findByChatIdAndId(CHAT_ID, CURRENT_ROOM_ID)).thenReturn(roomDocument);

        val actualRoom = roomService.getRoomByIdAndChatId(CHAT_ID, CURRENT_ROOM_ID);

        assertEquals(roomDocument.getChatId(), actualRoom.getChatId());
        assertEquals(roomDocument.getId(), actualRoom.getId());

    }

    @Test
    @DisplayName("Successfully updates room with not persisted content")
    void saveOrUpdateRoomWithNewRoomContent() {
        val room = new Room();
        room.setId(CURRENT_ROOM_ID);
        room.setChatId(CHAT_ID);
        val roomContent = new EmptyRoom(RoomType.NORMAL);
        room.setRoomContent(roomContent);

        roomService.saveOrUpdateRoom(room);

        ArgumentCaptor<RoomContentDocument> roomContentDocumentArgumentCaptor =
                ArgumentCaptor.forClass(RoomContentDocument.class);
        verify(roomContentRepository).save(roomContentDocumentArgumentCaptor.capture());

        val actualRoomContent = roomContentDocumentArgumentCaptor.getValue();
        assertEquals(RoomType.NORMAL, actualRoomContent.getRoomType());

        ArgumentCaptor<RoomDocument> roomDocumentArgumentCaptor =
                ArgumentCaptor.forClass(RoomDocument.class);
        verify(roomRepository).save(roomDocumentArgumentCaptor.capture());
        val actualRoom = roomDocumentArgumentCaptor.getValue();
        assertEquals(CURRENT_ROOM_ID, actualRoom.getId());
        assertEquals(CHAT_ID, actualRoom.getChatId());
    }

    @Test
    @DisplayName("Successfully updates room with already persisted content")
    void saveOrUpdateRoomWithPersistedRoomContent() {
        val room = new Room();
        room.setId(CURRENT_ROOM_ID);
        room.setChatId(CHAT_ID);
        val roomContent = new EmptyRoom(RoomType.NORMAL);
        roomContent.setId("roomContentId");
        room.setRoomContent(roomContent);

        roomService.saveOrUpdateRoom(room);

        ArgumentCaptor<RoomDocument> roomDocumentArgumentCaptor =
                ArgumentCaptor.forClass(RoomDocument.class);
        verify(roomRepository).save(roomDocumentArgumentCaptor.capture());
        val actualRoom = roomDocumentArgumentCaptor.getValue();
        assertEquals(CURRENT_ROOM_ID, actualRoom.getId());
        assertEquals(CHAT_ID, actualRoom.getChatId());
        assertEquals("roomContentId", actualRoom.getRoomContent().getId());
    }

    @Test
    @DisplayName("Successfully saves or updates room room content")
    void saveOrUpdateRoomContent() {
        val roomContent = new EmptyRoom(RoomType.NORMAL);

        roomService.saveOrUpdateRoomContent(roomContent);

        ArgumentCaptor<RoomContentDocument> roomContentDocumentArgumentCaptor =
                ArgumentCaptor.forClass(RoomContentDocument.class);
        verify(roomContentRepository).save(roomContentDocumentArgumentCaptor.capture());
        val actualRoomContentDocument = roomContentDocumentArgumentCaptor.getValue();

        assertEquals(RoomType.NORMAL, actualRoomContentDocument.getRoomType());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Successfully opens menu with list of merchant items to buy")
    void openMerchantBuyMenu() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val roomContent = getMerchant();
        val currentRoom = new Room();
        currentRoom.setRoomContent(roomContent);

        val actualResult = roomService.openMerchantBuyMenu(CHAT_ID, player, currentRoom);

        ArgumentCaptor<Set<Item>> argument = ArgumentCaptor.forClass(Set.class);
        verify(messageService).sendMerchantBuyMenuMessage(eq(CHAT_ID), eq(player.getGold()), argument.capture());
        val actualItems = argument.getValue();
        assertEquals(roomContent.getItems(), actualItems);
        assertTrue(actualResult);
    }

    @Test
    @DisplayName("Successfully opens menu with list of merchant's items to buy")
    void openMerchantSellMenu() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val room = new Room();
        val roomContent = getMerchant();
        room.setRoomContent(roomContent);
        room.setId(CURRENT_ROOM_ID);
        room.setChatId(CHAT_ID);

        val actualResult = roomService.openMerchantSellMenu(CHAT_ID, player, room);

        verify(messageService).sendMerchantSellMenuMessage(CHAT_ID, player);
        assertTrue(actualResult);
    }

    @Test
    @DisplayName("Successfully opens menu with list of player's items to sell")
    void openMerchantBuyItem() {
        val itemId = "itemId1";
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val room = new RoomDocument();
        val roomContent = new RoomContentDocument();
        roomContent.setRoomType(RoomType.MERCHANT);
        roomContent.setItems(getMerchant().getItems().stream().map(item -> {
            val result = new ItemDocument();
            result.setItemType(item.getItemType());
            result.setId(item.getId());
            return result;
        }).collect(Collectors.toList()));
        room.setRoomContent(roomContent);

        when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
        when(roomRepository.findByChatIdAndId(CHAT_ID, CURRENT_ROOM_ID)).thenReturn(room);

        val actualResult = roomService.openMerchantBuyItem(CHAT_ID, itemId);

        ArgumentCaptor<Item> itemArgumentCaptor = ArgumentCaptor.forClass(Item.class);
        verify(messageService).sendMerchantBuyItemMessage(eq(CHAT_ID), itemArgumentCaptor.capture());
        val actualItem = itemArgumentCaptor.getValue();

        assertEquals(itemId, actualItem.getId());
        assertTrue(actualResult);
    }

    @Test
    @DisplayName("Successfully restores player's armor")
    void restoreArmor() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val room = new RoomDocument();
        room.setId(CURRENT_ROOM_ID);
        when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
        when(effectService.updateArmorEffect(player)).thenReturn(player);
        when(playerService.updatePlayer(player)).thenReturn(player);
        when(roomRepository.findByChatIdAndId(CHAT_ID, CURRENT_ROOM_ID)).thenReturn(room);

        ArgumentCaptor<Player> playerArgumentCaptor = ArgumentCaptor.forClass(Player.class);
        ArgumentCaptor<Room> roomArgumentCaptor = ArgumentCaptor.forClass(Room.class);
        when(messageService.sendRoomMessage(eq(CHAT_ID), playerArgumentCaptor.capture(), roomArgumentCaptor.capture())).thenReturn(true);

        val actualResult = roomService.restoreArmor(CHAT_ID);

        val actualPlayer = playerArgumentCaptor.getValue();
        val actualRoom = roomArgumentCaptor.getValue();
        assertTrue(actualResult);
        assertEquals(actualPlayer.getDefense(), actualPlayer.getMaxDefense());
        assertEquals(CURRENT_ROOM_ID, actualRoom.getId());

    }

    @Test
    @DisplayName("Successfully updates player's attribute")
    void upgradePlayerAttribute() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val expectedValue = player.getAttributes().get(PlayerAttribute.MAGIC) + 1;
        val room = new RoomDocument();
        room.setId(CURRENT_ROOM_ID);
        when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
        when(playerService.updatePlayer(player)).thenReturn(player);
        when(roomRepository.findByChatIdAndId(CHAT_ID, CURRENT_ROOM_ID)).thenReturn(room);

        ArgumentCaptor<Player> playerArgumentCaptor = ArgumentCaptor.forClass(Player.class);
        ArgumentCaptor<Room> roomArgumentCaptor = ArgumentCaptor.forClass(Room.class);
        when(messageService.sendRoomMessage(eq(CHAT_ID), playerArgumentCaptor.capture(), roomArgumentCaptor.capture())).thenReturn(true);

        val actualResult = roomService.upgradePlayerAttribute(CHAT_ID, PlayerAttribute.MAGIC);

        val actualPlayer = playerArgumentCaptor.getValue();
        val actualRoom = roomArgumentCaptor.getValue();
        assertTrue(actualResult);
        assertEquals(expectedValue, actualPlayer.getAttributes().get(PlayerAttribute.MAGIC));
        assertEquals(CURRENT_ROOM_ID, actualRoom.getId());
    }
}