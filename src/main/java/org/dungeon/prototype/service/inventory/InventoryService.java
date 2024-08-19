package org.dungeon.prototype.service.inventory;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.*;
import org.dungeon.prototype.aspect.dto.InventoryItemResponseDto;
import org.dungeon.prototype.model.inventory.ArmorSet;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.WeaponSet;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.repository.ArmorSetRepository;
import org.dungeon.prototype.repository.InventoryRepository;
import org.dungeon.prototype.repository.WeaponSetRepository;
import org.dungeon.prototype.repository.converters.mapstruct.ArmorSetMapper;
import org.dungeon.prototype.repository.converters.mapstruct.InventoryMapper;
import org.dungeon.prototype.repository.converters.mapstruct.WeaponSetMapper;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.util.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Service
public class InventoryService {
    @Autowired
    PlayerService playerService;
    @Autowired
    ItemService itemService;
    @Autowired
    RoomService roomService;
    @Autowired
    InventoryRepository inventoryRepository;
    @Autowired
    ArmorSetRepository armorSetRepository;
    @Autowired
    WeaponSetRepository weaponSetRepository;

    public Inventory getDefaultInventory(Long chatId) {
        Inventory inventory = new Inventory();
        inventory.setItems(new ArrayList<>());
        inventory.setArmorSet(getDefaultArmorSet(chatId));
        inventory.setWeaponSet(getDefaultWeaponSet(chatId));
        inventory = saveOrUpdateInventory(inventory);
        return inventory;
    }

    public Inventory saveOrUpdateInventory(Inventory inventory) {
        inventory.setArmorSet(saveOrUpdateArmorSet(inventory.getArmorSet()));
        inventory.setWeaponSet(saveOrUpdateWeaponSet(inventory.getWeaponSet()));
        val inventoryDocument = InventoryMapper.INSTANCE.mapToDocument(inventory);
        val savedInventoryDocument = inventoryRepository.save(inventoryDocument);
        return InventoryMapper.INSTANCE.mapToEntity(savedInventoryDocument);
    }

    @SendTreasureMessage
    public boolean collectAllTreasure(Long chatId) {
        var player = playerService.getPlayer(chatId);
        var currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
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
                return true;
            } else {
                treasure.setItems(Collections.emptySet());
            }
        }
        playerService.updatePlayer(player);
        roomService.saveOrUpdateRoom(currentRoom);
        saveOrUpdateInventory(player.getInventory());
        return true;
    }

    @SendTreasureMessage
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
            roomService.saveOrUpdateRoom(currentRoom);
            playerService.updatePlayer(player);
            saveOrUpdateInventory(player.getInventory());
        } else {
            log.info("No room in inventory!");
            return false;
        }
        return true;
    }

    @SendInventoryMessage
    public InventoryItemResponseDto sendOrUpdateInventoryMessage(Long chatId) {
        return InventoryItemResponseDto.builder()
                .chatId(chatId)
                .inventoryType(CallbackType.INVENTORY)
                .build();
    }

    @SendInventoryMessage
    public InventoryItemResponseDto equipItem(Long chatId, String itemId) {
        val player = playerService.getPlayer(chatId);
        val item = itemService.findItem(chatId, itemId);
        val inventory = player.getInventory();
        val result = equipItem(inventory, item);
        return InventoryItemResponseDto.builder()
                .chatId(chatId)
                .itemId(itemId)
                .inventoryType(CallbackType.INVENTORY)
                .isOk(result)
                .build();
    }

    @SendInventoryMessage
    public InventoryItemResponseDto unEquipItem(Long chatId, String itemId) {
        val player = playerService.getPlayer(chatId);
        val item= itemService.findItem(chatId, itemId);
        val inventory = player.getInventory();
        if ((inventory.getMaxItems().equals(inventory.getItems().size()))) {
            //TODO implement prompt
            return InventoryItemResponseDto.builder()
                    .chatId(chatId)
                    .itemId(itemId)
                    .inventoryType(CallbackType.INVENTORY)
                    .isOk(false)
                    .build();
        }
        val result = unEquipItem(item, inventory);
        if (result) {
            player.removeItemEffects(item.getEffects());
        }
        return InventoryItemResponseDto.builder()
                .chatId(chatId)
                .itemId(itemId)
                .inventoryType(CallbackType.INVENTORY)
                .isOk(result)
                .build();
    }

    @SendMerchantSellMenuMessage
    public boolean sellItem(Long chatId, String itemId) {
        val player = playerService.getPlayer(chatId);
        val item = itemService.findItem(chatId, itemId);
        val inventory = player.getInventory();
        val result = inventory.removeItem(item);
        if (result) {
            player.removeItemEffects(item.getEffects());
            player.addGold(item.getSellingPrice());
            saveOrUpdateInventory(inventory);
            playerService.updatePlayer(player);
        }
        return result;
    }

    @SendMerchantBuyMenuMessage
    public boolean buyItem(Long chatId, String itemId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (player.getInventory().isFull()) {
            log.warn("Inventory is full!");
            return false;
        }
        val item = itemService.findItem(chatId, itemId);
        if (player.getGold() < item.getBuyingPrice()) {
            log.warn("Not enough money!");
            return false;
        }
        player.getInventory().addItem(item);
        ((Merchant) currentRoom.getRoomContent()).getItems().remove(item);
        player.removeGold(item.getSellingPrice());
        playerService.updatePlayer(player);
        roomService.saveOrUpdateRoom(currentRoom);
        saveOrUpdateInventory(player.getInventory());
        return true;
    }

    @SendInventoryItem
    public InventoryItemResponseDto openInventoryItemInfo(Long chatId, String itemId, CallbackType inventoryType, Optional<CallbackType> callbackType) {
        return InventoryItemResponseDto.builder()
                .chatId(chatId)
                .itemId(itemId)
                .inventoryType(inventoryType)
                .itemType(callbackType.map(MessageUtil::formatItemType).orElse(null))
                .build();
    }

    private WeaponSet saveOrUpdateWeaponSet(WeaponSet weaponSet) {
        val weaponSetDocument = WeaponSetMapper.INSTANCE.mapToDocument(weaponSet);
        val savedWeaponSetDocument = weaponSetRepository.save(weaponSetDocument);
        return WeaponSetMapper.INSTANCE.mapToEntity(savedWeaponSetDocument);
    }

    private ArmorSet saveOrUpdateArmorSet(ArmorSet armorSet) {
        val armorSetDocument = ArmorSetMapper.INSTANCE.mapToDocument(armorSet);
        val savedArmorSetDocument = armorSetRepository.save(armorSetDocument);
        return ArmorSetMapper.INSTANCE.mapToEntity(savedArmorSetDocument);
    }

    private ArmorSet getDefaultArmorSet(Long chatId) {
        val vest = itemService.getMostLightweightWearable(chatId, WearableType.VEST);
        val armorSet = new ArmorSet();
        armorSet.setVest(vest);
        return armorSet;
    }

    private WeaponSet getDefaultWeaponSet(Long chatId) {
        WeaponSet weaponSet = new WeaponSet();
        val weapon = itemService.getMostLightWeightMainWeapon(chatId);
        weaponSet.addWeapon(weapon);
        return weaponSet;
    }

    public boolean equipItem(Inventory inventory, Item item) {
        if (isNull(item)) {
            return false;
        }
        return switch (item.getItemType()) {
            case WEAPON -> processWeaponEquip(inventory, (Weapon) item);
            case WEARABLE -> processWearableEquip(inventory, (Wearable) item);
            case USABLE -> processUsable(inventory);
        };
    }

    public boolean unEquipItem(Item item, Inventory inventory) {
        val unEquippedItem = inventory.unEquip(item);
        if (unEquippedItem.isPresent() && inventory.addItem(unEquippedItem.get())) {
            saveOrUpdateInventory(inventory);
            return true;
        } else {
            return false;
        }
    }

    private boolean processUsable(Inventory inventory) {
        //TODO: consider mocking (since considering using Usables directly from inventory list
        return true;
    }

    private boolean processWearableEquip(Inventory inventory, Wearable wearable) {
        val armorSet = inventory.getArmorSet();
        switch (wearable.getAttributes().getWearableType()) {
            case HELMET -> {
                inventory.addItem(armorSet.getHelmet());
                armorSet.setHelmet(wearable);
            }
            case VEST -> {
                inventory.addItem(armorSet.getVest());
                armorSet.setVest(wearable);
            }
            case GLOVES -> {
                inventory.addItem(armorSet.getGloves());
                armorSet.setGloves(wearable);
            }
            case BOOTS -> {
                inventory.addItem(armorSet.getBoots());
                armorSet.setBoots(wearable);
            }
        }
        inventory.getItems().remove(wearable);
        saveOrUpdateArmorSet(armorSet);
        saveOrUpdateInventory(inventory);
        return true;
    }

    private boolean processWeaponEquip(Inventory inventory, Weapon weapon) {
        var weaponSet = inventory.getWeaponSet();
        switch (weapon.getAttributes().getHandling()) {
            case SINGLE_HANDED -> {
                if (nonNull(weaponSet.getPrimaryWeapon())) {
                    if (nonNull(weaponSet.getSecondaryWeapon())) {
                        inventory.addItem(weaponSet.getSecondaryWeapon());
                    }
                    weaponSet.setSecondaryWeapon(weapon);
                } else {
                    weaponSet.setPrimaryWeapon(weapon);
                }
            }
            case TWO_HANDED -> {
                if (nonNull(weaponSet.getPrimaryWeapon())) {
                    inventory.addItem(weaponSet.getPrimaryWeapon());
                    weaponSet.setPrimaryWeapon(null);
                }
                if (nonNull(weaponSet.getSecondaryWeapon())) {
                    inventory.addItem(weaponSet.getSecondaryWeapon());
                    weaponSet.setSecondaryWeapon(null);
                }
                weaponSet.setPrimaryWeapon(weapon);
            }
        }
        inventory.getItems().remove(weapon);
        saveOrUpdateWeaponSet(weaponSet);
        saveOrUpdateInventory(inventory);
        return true;
    }
}
