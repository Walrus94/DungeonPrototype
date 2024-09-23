package org.dungeon.prototype.service.room;

import lombok.val;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.service.BaseServiceUnitTest;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.inventory.InventoryService;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.message.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.HashSet;
import java.util.Set;

import static org.dungeon.prototype.TestData.getItems;
import static org.dungeon.prototype.TestData.getPlayer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class TreasureServiceTest extends BaseServiceUnitTest {
    @InjectMocks
    TreasureService treasureService;
    @Mock
    PlayerService playerService;
    @Mock
    LevelService levelService;
    @Mock
    RoomService roomService;
    @Mock
    InventoryService inventoryService;
    @Mock
    MessageService messageService;

    @Test
    @DisplayName("Successfully opens treasure")
    void openTreasure() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val room = new Room();
        val roomContent = new Treasure();
        roomContent.setGold(100);
        roomContent.setItems(getItems());
        room.setRoomContent(roomContent);

        when(roomService.getRoomByIdAndChatId(CHAT_ID, CURRENT_ROOM_ID)).thenReturn(room);

        treasureService.openTreasure(CHAT_ID, player);

        verify(messageService).sendTreasureMessage(CHAT_ID, roomContent);
    }

    @Test
    @DisplayName("Fails to open looted treasure, sends room message instead")
    void openLootedTreasureAndSendRoomMessage() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val room = new Room();
        val roomContent = new Treasure();
        roomContent.setGold(0);
        roomContent.setItems(new HashSet<>());
        room.setRoomContent(roomContent);

        when(roomService.getRoomByIdAndChatId(CHAT_ID, CURRENT_ROOM_ID)).thenReturn(room);
        doNothing().when(levelService).updateAfterTreasureLooted(room);

        treasureService.openTreasure(CHAT_ID, player);

        verify(messageService).sendRoomMessage(CHAT_ID, player, room);
    }


    @Test
    @DisplayName("Successfully collects gold and sends room message since treasure is looted")
    void collectTreasureGoldNoItemsLeft() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val currentRoom = new Room();
        val roomContent = new Treasure();
        roomContent.setGold(100);
        roomContent.setItems(new HashSet<>());
        currentRoom.setRoomContent(roomContent);

        ArgumentCaptor<Player> playerArgumentCaptor = ArgumentCaptor.forClass(Player.class);
        when(playerService.updatePlayer(playerArgumentCaptor.capture())).thenReturn(player);
        doNothing().when(messageService).sendRoomMessage(eq(CHAT_ID), eq(player), any(Room.class));

        treasureService.collectTreasureGold(CHAT_ID, player, currentRoom);

        val actualPlayer = playerArgumentCaptor.getValue();
        assertEquals(200, actualPlayer.getGold());

        ArgumentCaptor<Room> roomArgumentCaptor = ArgumentCaptor.forClass(Room.class);
        verify(levelService).updateAfterTreasureLooted(roomArgumentCaptor.capture());
        val actualRoom = roomArgumentCaptor.getValue();
        assertEquals(0, ((Treasure) actualRoom.getRoomContent()).getGold());
        assertTrue(((Treasure) actualRoom.getRoomContent()).getItems().isEmpty());
    }

    @Test
    @DisplayName("Successfully collecting only gold, leaving items in treasure")
    void collectTreasureGoldItemsLeft() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val currentRoom = new Room();
        val roomContent = new Treasure();
        roomContent.setGold(100);
        roomContent.setItems(getItems());
        currentRoom.setRoomContent(roomContent);


        ArgumentCaptor<Player> playerArgumentCaptor = ArgumentCaptor.forClass(Player.class);
        when(playerService.updatePlayer(playerArgumentCaptor.capture())).thenReturn(player);

        treasureService.collectTreasureGold(CHAT_ID, player, currentRoom);

        ArgumentCaptor<Treasure> treasureArgumentCaptor = ArgumentCaptor.forClass(Treasure.class);
        verify(messageService).sendTreasureMessage(eq(CHAT_ID), treasureArgumentCaptor.capture());
        val actualPlayer = playerArgumentCaptor.getValue();
        val actualTreasure = treasureArgumentCaptor.getValue();
        assertEquals(200, actualPlayer.getGold());
        assertEquals(0, actualTreasure.getGold());
        assertEquals(roomContent.getItems(), actualTreasure.getItems());

    }

    @Test
    @DisplayName("Successfully collects all treasure and sends updated room message")
    void collectAllTreasure() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val currentRoom = new Room();
        val roomContent = new Treasure();
        roomContent.setGold(100);
        roomContent.setItems(getItems());
        currentRoom.setRoomContent(roomContent);

        ArgumentCaptor<Player> playerArgumentCaptor = ArgumentCaptor.forClass(Player.class);
        when(playerService.updatePlayer(playerArgumentCaptor.capture())).thenReturn(player);
        ArgumentCaptor<Inventory> inventoryArgumentCaptor = ArgumentCaptor.forClass(Inventory.class);
        doNothing().when(messageService).sendRoomMessage(eq(CHAT_ID), any(Player.class), any(Room.class));

        when(inventoryService.saveOrUpdateInventory(inventoryArgumentCaptor.capture())).thenReturn(player.getInventory());

        treasureService.collectAllTreasure(CHAT_ID, player, currentRoom);

        ArgumentCaptor<Room> roomArgumentCaptor = ArgumentCaptor.forClass(Room.class);
        verify(levelService).updateAfterTreasureLooted(roomArgumentCaptor.capture());
        val actualPlayer = playerArgumentCaptor.getValue();
        val actualRoom = roomArgumentCaptor.getValue();
        val actualInventory = inventoryArgumentCaptor.getValue();

        assertEquals(200, actualPlayer.getGold());
        assertEquals(0, ((Treasure) actualRoom.getRoomContent()).getGold());
        assertTrue(((Treasure) actualRoom.getRoomContent()).getItems().isEmpty());
        assertTrue(actualInventory.getItems().containsAll(roomContent.getItems()));

    }

    @Test
    @DisplayName("Collect last item of treasure")
    void collectLastTreasureItemNoGoldLeft() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val room = new Room();
        val roomContent = new Treasure();
        roomContent.setGold(0);
        val item = new Weapon();
        item.setId("itemId");
        Set<Item> items = new HashSet<>();
        items.add(item);
        roomContent.setItems(items);
        room.setRoomContent(roomContent);

        when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
        when(roomService.getRoomByIdAndChatId(CHAT_ID, CURRENT_ROOM_ID)).thenReturn(room);
        ArgumentCaptor<Player> playerArgumentCaptor = ArgumentCaptor.forClass(Player.class);
        when(playerService.updatePlayer(playerArgumentCaptor.capture())).thenReturn(player);
        ArgumentCaptor<Inventory> inventoryArgumentCaptor = ArgumentCaptor.forClass(Inventory.class);
        when(inventoryService.saveOrUpdateInventory(inventoryArgumentCaptor.capture())).thenReturn(player.getInventory());

        val actualResult = treasureService.collectTreasureItem(CHAT_ID, "itemId");

        ArgumentCaptor<Room> roomArgumentCaptor = ArgumentCaptor.forClass(Room.class);
        verify(levelService).updateAfterTreasureLooted(roomArgumentCaptor.capture());

        assertTrue(actualResult);
        val actualPlayer = playerArgumentCaptor.getValue();
        val actualRoom = roomArgumentCaptor.getValue();
        val actualInventory = inventoryArgumentCaptor.getValue();
        assertEquals(100, actualPlayer.getGold());
        assertEquals(0, ((Treasure) actualRoom.getRoomContent()).getGold());
        assertTrue(((Treasure) actualRoom.getRoomContent()).getItems().isEmpty());
        assertTrue(actualInventory.getItems().containsAll(roomContent.getItems()));
    }

    @Test
    @DisplayName("Collect last item of treasure where gold left")
    void collectTreasureItemGoldLeft() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val room = new Room();
        val roomContent = new Treasure();
        roomContent.setGold(100);
        val item = new Weapon();
        item.setId("itemId");
        Set<Item> items = new HashSet<>();
        items.add(item);
        roomContent.setItems(items);
        room.setRoomContent(roomContent);

        when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
        when(roomService.getRoomByIdAndChatId(CHAT_ID, CURRENT_ROOM_ID)).thenReturn(room);
        ArgumentCaptor<Room> roomArgumentCaptor = ArgumentCaptor.forClass(Room.class);
        when(roomService.saveOrUpdateRoom(roomArgumentCaptor.capture())).thenReturn(room);
        ArgumentCaptor<Player> playerArgumentCaptor = ArgumentCaptor.forClass(Player.class);
        when(playerService.updatePlayer(playerArgumentCaptor.capture())).thenReturn(player);
        ArgumentCaptor<Inventory> inventoryArgumentCaptor = ArgumentCaptor.forClass(Inventory.class);
        when(inventoryService.saveOrUpdateInventory(inventoryArgumentCaptor.capture())).thenReturn(player.getInventory());

        val actualResult = treasureService.collectTreasureItem(CHAT_ID, "itemId");

        assertTrue(actualResult);
        val actualPlayer = playerArgumentCaptor.getValue();
        val actualRoom = roomArgumentCaptor.getValue();
        val actualInventory = inventoryArgumentCaptor.getValue();
        assertEquals(100, actualPlayer.getGold());
        assertEquals(100, ((Treasure) actualRoom.getRoomContent()).getGold());
        assertTrue(((Treasure) actualRoom.getRoomContent()).getItems().isEmpty());
        assertTrue(actualInventory.getItems().containsAll(roomContent.getItems()));
    }
}