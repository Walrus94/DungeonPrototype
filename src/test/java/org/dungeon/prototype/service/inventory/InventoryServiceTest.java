package org.dungeon.prototype.service.inventory;

import lombok.val;
import org.dungeon.prototype.async.AsyncJobService;
import org.dungeon.prototype.async.TaskType;
import org.dungeon.prototype.model.document.player.InventoryDocument;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.dungeon.prototype.model.inventory.attributes.Quality;
import org.dungeon.prototype.model.inventory.attributes.weapon.Handling;
import org.dungeon.prototype.model.inventory.attributes.weapon.Size;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponType;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.repository.mongo.InventoryRepository;
import org.dungeon.prototype.service.BaseServiceUnitTest;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.util.GenerationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static org.dungeon.prototype.TestData.getPlayer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceTest extends BaseServiceUnitTest {

    private static final String ITEM_ID = "item_id";

    @InjectMocks
    private InventoryService inventoryService;

    @Mock
    private PlayerService playerService;
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
    @Mock
    private AsyncJobService asyncJobHandler;

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
        vest.setArmor(5);
        vest.setMagicType(MagicType.of(0.0,0.0));
        vest.setId("vest_id");
        expectedInventory.setVest(vest);
        val weapon = new Weapon();
        val weaponAttributes = new WeaponAttributes();
        weaponAttributes.setWeaponType(WeaponType.SWORD);
        weaponAttributes.setQuality(Quality.COMMON);
        weaponAttributes.setSize(Size.MEDIUM);
        weaponAttributes.setWeaponAttackType(WeaponAttackType.SLASH);
        weaponAttributes.setHandling(Handling.SINGLE_HANDED);
        weapon.setAttributes(weaponAttributes);
        weapon.setId("weapon_id");
        weapon.setAttack(5);
        weapon.setChanceToMiss(0.0);
        weapon.setChanceToKnockOut(0.0);
        weapon.setCriticalHitChance(0.0);
        weapon.setMagicType(MagicType.of(0.0,0.0));
        weapon.setCriticalHitMultiplier(1.0);
        weapon.setEffects(new ArrayList<>());
        expectedInventory.setPrimaryWeapon(weapon);
        expectedInventory.setItems(new ArrayList<>());

        when(asyncJobHandler.submitTask(any(Callable.class), eq(TaskType.GET_DEFAULT_INVENTORY), eq(CHAT_ID))).thenReturn(CompletableFuture.completedFuture(expectedInventory));

        Inventory actualInventory = inventoryService.getDefaultInventory(CHAT_ID);


        assertEquals(expectedInventory.getPrimaryWeapon().getId(), actualInventory.getPrimaryWeapon().getId());
        assertEquals(expectedInventory.getPrimaryWeapon().getAttack(), actualInventory.getPrimaryWeapon().getAttack());
        assertEquals(expectedInventory.getPrimaryWeapon().getAttributes(), actualInventory.getPrimaryWeapon().getAttributes());

        assertEquals(expectedInventory.getVest().getId(), actualInventory.getVest().getId());
        assertEquals(expectedInventory.getVest().getArmor(), actualInventory.getVest().getArmor());
        assertEquals(expectedInventory.getVest().getAttributes(), actualInventory.getVest().getAttributes());
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

        inventoryService.sendInventoryMessage(CHAT_ID, player);

        verify(messageService).sendInventoryMessage(CHAT_ID, player.getInventory());
    }

    @Test
    @DisplayName("Successfully processes inventory item equipment and send corresponding message")
    void equipItem() {
        val player = getPlayer(CHAT_ID);
        val item = new Wearable();
        item.setId(ITEM_ID);
        WearableAttributes wearableAttributes = new WearableAttributes();
        wearableAttributes.setWearableType(WearableType.GLOVES);
        wearableAttributes.setQuality(Quality.COMMON);
        wearableAttributes.setWearableMaterial(WearableMaterial.CLOTH);
        item.setAttributes(wearableAttributes);
        item.setEffects(new ArrayList<>());
        item.setArmor(0);
        item.setMagicType(MagicType.of(0.0,0.0));
        player.getInventory().addItem(item);

        val inventoryDocument = new InventoryDocument();

        when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
        when(itemService.findItem(CHAT_ID, ITEM_ID)).thenReturn(item);
        when(effectService.updateArmorEffect(player)).thenReturn(player);
        when(inventoryRepository.save(any())).thenReturn(inventoryDocument);

        inventoryService.equipItem(CHAT_ID, ITEM_ID);

        ArgumentCaptor<Inventory> inventoryArgumentCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(playerService).updatePlayer(player);
        verify(messageService).sendInventoryMessage(eq(CHAT_ID), inventoryArgumentCaptor.capture());

        val actualInventory = inventoryArgumentCaptor.getValue();
        assertEquals(item, player.getInventory().getGloves());
        assertTrue(actualInventory.getItems().isEmpty());
        assertEquals(ITEM_ID, actualInventory.getGloves().getId());
    }

    @Test
    @DisplayName("Successfully processes inventory item un-equipment and send corresponding message")
    void unEquipItem() {
        val player = getPlayer(CHAT_ID);
        val item = player.getInventory().getPrimaryWeapon();
        when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
        when(itemService.findItem(CHAT_ID, "weapon_id")).thenReturn(item);

        ArgumentCaptor<InventoryDocument> inventoryDocumentArgumentCaptor = ArgumentCaptor.forClass(InventoryDocument.class);
        when(inventoryRepository.save(inventoryDocumentArgumentCaptor.capture())).thenReturn(new InventoryDocument());

        inventoryService.unEquipItem(CHAT_ID, "weapon_id");

        verify(effectService).updateArmorEffect(player);
        verify(messageService).sendInventoryMessage(eq(CHAT_ID), any(Inventory.class));
        val actualInventory = inventoryDocumentArgumentCaptor.getValue();
        assertEquals(1L, actualInventory.getItems().stream().filter(itemDocument -> "weapon_id".equals(itemDocument.getId())).count());
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
        item.setArmor(5);
        item.setMagicType(MagicType.of(0.0, 0.0));
        item.setEffects(new ArrayList<>());
        player.getInventory().addItem(item);

        try(MockedStatic<GenerationUtil> generationUtilMockedStatic = mockStatic(GenerationUtil.class)) {
            when(GenerationUtil.calculateWearableWeight(anyInt(), anyDouble(), any(MagicType.class))).thenReturn(
                    Weight.builder()
                            .armor(5.0)
                            .build());
            when(GenerationUtil.calculateWeaponWeight(anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(MagicType.class))).thenReturn(
                    Weight.builder()
                            .attack(5.0)
                            .build()
            );

            when(GenerationUtil.getBuyingPriceRatio()).thenReturn(1.2);
            when(GenerationUtil.getSellingPriceRatio()).thenReturn(0.9);
            
            when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
            when(itemService.findItem(CHAT_ID, ITEM_ID)).thenReturn(item);

            ArgumentCaptor<InventoryDocument> inventoryDocumentArgumentCaptor = ArgumentCaptor.forClass(InventoryDocument.class);
            when(inventoryRepository.save(inventoryDocumentArgumentCaptor.capture())).thenReturn(new InventoryDocument());

            ArgumentCaptor<Player> playerArgumentCaptor = ArgumentCaptor.forClass(Player.class);
            when(playerService.updatePlayer(playerArgumentCaptor.capture())).thenReturn(player);

            inventoryService.sellItem(CHAT_ID, ITEM_ID);

            val actualInventory = inventoryDocumentArgumentCaptor.getValue();
            val actualPlayer = playerArgumentCaptor.getValue();
            assertEquals(104, actualPlayer.getGold());
            assertTrue(actualInventory.getItems().isEmpty());
            verify(messageService).sendMerchantSellMenuMessage(CHAT_ID, player);
        }
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
        item.setArmor(5);
        item.setMagicType(MagicType.of(0.0,0.0));
        item.setEffects(new ArrayList<>());
        Set<Item> items = new HashSet<>();
        items.add(item);
        merchant.setItems(items);
        room.setRoomContent(merchant);

        try(MockedStatic<GenerationUtil> generationUtilMockedStatic = mockStatic(GenerationUtil.class)) {
            when(GenerationUtil.calculateWearableWeight(anyInt(), anyDouble(), any(MagicType.class))).thenReturn(
                    Weight.builder()
                            .armor(5.0)
                            .build());
            when(GenerationUtil.calculateWeaponWeight(anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(MagicType.class))).thenReturn(
                    Weight.builder()
                            .attack(5.0)
                            .build()
            );

            when(GenerationUtil.getBuyingPriceRatio()).thenReturn(1.2);
            when(GenerationUtil.getSellingPriceRatio()).thenReturn(0.9);

            when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
            when(roomService.getRoomByIdAndChatId(CHAT_ID, CURRENT_ROOM_ID)).thenReturn(room);
            when(itemService.findItem(CHAT_ID, ITEM_ID)).thenReturn(item);

            inventoryService.buyItem(CHAT_ID, ITEM_ID);

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
            assertEquals(96, actualPlayer.getGold());
            assertEquals(1, actualInventory.getItems().stream()
                    .filter(itemDocument -> ITEM_ID.equals(itemDocument.getId())).count());
            assertTrue(actualRoom.getRoomContent() instanceof Merchant);
            assertFalse(((Merchant) actualRoom.getRoomContent()).getItems().contains(item));

            verify(messageService).sendMerchantBuyMenuMessage(CHAT_ID, actualPlayer.getGold(),
                    ((Merchant) actualRoom.getRoomContent()).getItems());
        }
    }

    @Test
    @DisplayName("Successfully sends inventory item message")
    void openInventoryItemInfo() {
        val player = getPlayer(CHAT_ID);
        val item = player.getInventory().getPrimaryWeapon();

        when(itemService.findItem(CHAT_ID, ITEM_ID)).thenReturn(item);

        inventoryService.openInventoryItemInfo(CHAT_ID, ITEM_ID, CallbackType.INVENTORY, Optional.empty());

        verify(messageService).sendInventoryItemMessage(CHAT_ID, item, CallbackType.INVENTORY, Optional.empty());
    }
}