package org.dungeon.prototype.service.inventory;

import lombok.val;
import org.dungeon.prototype.model.document.item.ItemDocument;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.document.player.InventoryDocument;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.Quality;
import org.dungeon.prototype.model.inventory.attributes.weapon.*;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.repository.InventoryRepository;
import org.dungeon.prototype.service.BaseServiceUnitTest;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.RoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.dungeon.prototype.TestData.getPlayer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceTest extends BaseServiceUnitTest {

    private static final String ITEM_ID = "item_id";

    @InjectMocks
    private InventoryService inventoryService;

    @Mock
    PlayerService playerService;
    @Mock
    private ItemService itemService;
    @Mock
    private RoomService roomService;
    @Mock
    private EffectService effectService;
    @Mock
    private MessageService messageService;
    @Mock
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("Sets default inventory with items from repository and returns it")
    void getDefaultInventory() {
        val expectedInventory = new Inventory();
        val vest = new Wearable();
        val wearableAttribute = new WearableAttributes();
        wearableAttribute.setWearableType(WearableType.VEST);
        wearableAttribute.setQuality(Quality.COMMON);
        wearableAttribute.setWearableMaterial(WearableMaterial.CLOTH);
        vest.setAttributes(wearableAttribute);
        vest.setEffects(new ArrayList<>());
        expectedInventory.setVest(vest);
        val weapon = new Weapon();
        val weaponAttributes = new WeaponAttributes();
        weaponAttributes.setWeaponType(WeaponType.SWORD);
        weaponAttributes.setQuality(Quality.COMMON);
        weaponAttributes.setSize(Size.MEDIUM);
        weaponAttributes.setWeaponAttackType(WeaponAttackType.SLASH);
        weaponAttributes.setHandling(Handling.SINGLE_HANDED);
        weapon.setAttributes(weaponAttributes);
        weapon.setEffects(new ArrayList<>());
        expectedInventory.setPrimaryWeapon(weapon);
        expectedInventory.setItems(new ArrayList<>());

        //TODO: replace with argument catcher
        val inventoryDocument = new InventoryDocument();
        val vestDocument = new ItemDocument();
        vestDocument.setEffects(new ArrayList<>());
        vestDocument.setItemType(ItemType.WEARABLE);
        vestDocument.setAttributes(wearableAttribute);
        inventoryDocument.setVest(vestDocument);
        val weaponDocument = new ItemDocument();
        weaponDocument.setEffects(new ArrayList<>());
        weaponDocument.setItemType(ItemType.WEAPON);
        weaponDocument.setAttributes(weaponAttributes);
        inventoryDocument.setPrimaryWeapon(weaponDocument);
        inventoryDocument.setItems(new ArrayList<>());

        when(itemService.getMostLightweightWearable(CHAT_ID, WearableType.VEST)).thenReturn(vest);
        when(itemService.getMostLightWeightMainWeapon(CHAT_ID)).thenReturn(weapon);
        when(inventoryRepository.save(inventoryDocument)).thenReturn(inventoryDocument);

        val actualInventory = inventoryService.getDefaultInventory(CHAT_ID);

        assertEquals(expectedInventory, actualInventory);
    }

    @Test
    @DisplayName("Successfully saves inventory to repository and returns saved value")
    void saveOrUpdateInventory() {
        val expectedInventory = new Inventory();
        expectedInventory.setItems(new ArrayList<>());

        val inventoryDocument = new InventoryDocument();
        inventoryDocument.setItems(new ArrayList<>());

        when(inventoryRepository.save(any(InventoryDocument.class))).thenReturn(inventoryDocument);

        val actualInventory = inventoryService.saveOrUpdateInventory(expectedInventory);

        assertEquals(expectedInventory, actualInventory);
    }

    @Test
    @DisplayName("Successfully sends data to prepare inventory menu message")
    void sendOrUpdateInventoryMessage() {
        val player = getPlayer(CHAT_ID);

        when(playerService.getPlayer(CHAT_ID)).thenReturn(player);

        inventoryService.sendOrUpdateInventoryMessage(CHAT_ID);

        verify(messageService).sendInventoryMessage(CHAT_ID, player.getInventory());
    }

    @Test
    @DisplayName("Successfully processes inventory item equipment and send corresponding message")
    void equipItem() {
        val player = getPlayer(CHAT_ID);
        val item = new Wearable();
        item.setId(ITEM_ID);
        WearableAttributes wearableAttributes = new WearableAttributes();
        wearableAttributes.setWearableType(WearableType.VEST);
        wearableAttributes.setQuality(Quality.COMMON);
        wearableAttributes.setWearableMaterial(WearableMaterial.CLOTH);
        item.setAttributes(wearableAttributes);
        item.setEffects(new ArrayList<>());
        player.getInventory().addItem(item);

        val inventoryDocument = new InventoryDocument();

        when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
        when(itemService.findItem(CHAT_ID, ITEM_ID)).thenReturn(item);
        when(effectService.updateArmorEffect(player)).thenReturn(player);
        when(inventoryRepository.save(any())).thenReturn(inventoryDocument);

        val actualResult = inventoryService.equipItem(CHAT_ID, ITEM_ID);

        ArgumentCaptor<Inventory> inventoryArgumentCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(playerService).updatePlayer(player);
        verify(messageService).sendInventoryMessage(eq(CHAT_ID), inventoryArgumentCaptor.capture());


        assertTrue(actualResult);

        val actualInventory = inventoryArgumentCaptor.getValue();
        assertEquals(item, player.getInventory().getVest());
        assertTrue(actualInventory.getItems().isEmpty());
        assertEquals(ITEM_ID, actualInventory.getVest().getId());
    }

    @Test
    @DisplayName("Successfully processes inventory item un-equipment and send corresponding message")
    void unEquipItem() {
        val player = getPlayer(CHAT_ID);
        val item = player.getInventory().getPrimaryWeapon();
        when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
        when(itemService.findItem(CHAT_ID, ITEM_ID)).thenReturn(item);

        ArgumentCaptor<InventoryDocument> inventoryDocumentArgumentCaptor = ArgumentCaptor.forClass(InventoryDocument.class);
        when(inventoryRepository.save(inventoryDocumentArgumentCaptor.capture())).thenReturn(new InventoryDocument());

        val actualResult = inventoryService.unEquipItem(CHAT_ID, ITEM_ID);

        assertTrue(actualResult);

        verify(effectService).updateArmorEffect(player);
        verify(messageService).sendInventoryMessage(eq(CHAT_ID), any(Inventory.class));
        val actualInventory = inventoryDocumentArgumentCaptor.getValue();
        assertEquals(1L, actualInventory.getItems().stream().filter(itemDocument -> ITEM_ID.equals(itemDocument.getId())).count());
        assertNull(actualInventory.getPrimaryWeapon());

    }

    @Test
    @DisplayName("Successfully sells item to merchant")
    void sellItem() {
        val player = getPlayer(CHAT_ID);
        val item = new Wearable();
        item.setId(ITEM_ID);
        WearableAttributes wearableAttributes = new WearableAttributes();
        wearableAttributes.setWearableType(WearableType.VEST);
        wearableAttributes.setQuality(Quality.COMMON);
        wearableAttributes.setWearableMaterial(WearableMaterial.CLOTH);
        item.setAttributes(wearableAttributes);
        item.setSellingPrice(10);
        item.setEffects(new ArrayList<>());
        player.getInventory().addItem(item);

        when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
        when(itemService.findItem(CHAT_ID, ITEM_ID)).thenReturn(item);

        ArgumentCaptor<InventoryDocument> inventoryDocumentArgumentCaptor = ArgumentCaptor.forClass(InventoryDocument.class);
        when(inventoryRepository.save(inventoryDocumentArgumentCaptor.capture())).thenReturn(new InventoryDocument());

        ArgumentCaptor<Player> playerArgumentCaptor = ArgumentCaptor.forClass(Player.class);
        when(playerService.updatePlayer(playerArgumentCaptor.capture())).thenReturn(player);

        val actualResult = inventoryService.sellItem(CHAT_ID, ITEM_ID);

        assertTrue(actualResult);
        val actualInventory = inventoryDocumentArgumentCaptor.getValue();
        val actualPlayer = playerArgumentCaptor.getValue();
        assertEquals(110, actualPlayer.getGold());
        assertTrue(actualInventory.getItems().isEmpty());
        verify(messageService).sendMerchantSellMenuMessage(CHAT_ID, player);
    }

    @Test
    @DisplayName("Successfully buys item from merchant")
    void buyItem() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val room = new Room();
        room.setId(CURRENT_ROOM_ID);
        val merchant = new Merchant();
        val item = new Wearable();
        item.setId(ITEM_ID);
        WearableAttributes wearableAttributes = new WearableAttributes();
        wearableAttributes.setWearableType(WearableType.VEST);
        wearableAttributes.setQuality(Quality.COMMON);
        wearableAttributes.setWearableMaterial(WearableMaterial.CLOTH);
        item.setAttributes(wearableAttributes);
        item.setBuyingPrice(10);
        item.setSellingPrice(5);
        item.setEffects(new ArrayList<>());
        Set<Item> items = new HashSet<>();
        items.add(item);
        merchant.setItems(items);
        room.setRoomContent(merchant);

        when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
        when(roomService.getRoomByIdAndChatId(CHAT_ID, CURRENT_ROOM_ID)).thenReturn(room);
        when(itemService.findItem(CHAT_ID, ITEM_ID)).thenReturn(item);

        val actualResult = inventoryService.buyItem(CHAT_ID, ITEM_ID);

        assertTrue(actualResult);
        ArgumentCaptor<Player> playerArgumentCaptor = ArgumentCaptor.forClass(Player.class);
        verify(playerService).updatePlayer(playerArgumentCaptor.capture());
        val actualPlayer = playerArgumentCaptor.getValue();
        ArgumentCaptor<Room> roomArgumentCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomService).saveOrUpdateRoom(roomArgumentCaptor.capture());
        val actualRoom = roomArgumentCaptor.getValue();
        ArgumentCaptor<InventoryDocument> inventoryDocumentArgumentCaptor =
                ArgumentCaptor.forClass(InventoryDocument.class);
        verify(inventoryRepository).save(inventoryDocumentArgumentCaptor.capture());
        val actualInventory = inventoryDocumentArgumentCaptor.getValue();
        assertEquals(95, actualPlayer.getGold());
        assertEquals(1, actualInventory.getItems().stream()
                .filter(itemDocument -> ITEM_ID.equals(itemDocument.getId())).count());
        assertTrue(actualRoom.getRoomContent() instanceof Merchant);
        assertFalse(((Merchant) actualRoom.getRoomContent()).getItems().contains(item));

        verify(messageService).sendMerchantBuyMenuMessage(CHAT_ID, actualPlayer.getGold(),
                ((Merchant) actualRoom.getRoomContent()).getItems());
    }

    @Test
    @DisplayName("Successfully sends inventory item message")
    void openInventoryItemInfo() {
        val player = getPlayer(CHAT_ID);
        val item = player.getInventory().getPrimaryWeapon();

        when(itemService.findItem(CHAT_ID, ITEM_ID)).thenReturn(item);

        val actualResult = inventoryService.openInventoryItemInfo(CHAT_ID, ITEM_ID, CallbackType.INVENTORY, Optional.empty());

        assertTrue(actualResult);
        verify(messageService).sendInventoryItemMessage(CHAT_ID, item, CallbackType.INVENTORY, Optional.empty());
    }
}