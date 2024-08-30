package org.dungeon.prototype.service.inventory;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.repository.InventoryRepository;
import org.dungeon.prototype.repository.converters.mapstruct.InventoryMapper;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.util.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Service
public class InventoryService {
    @Autowired
    private PlayerService playerService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private InventoryRepository inventoryRepository;

    /**
     * Builds default inventory for start of the game
     * @param chatId id of player's chat
     * @return inventory, containing primary weapon and vest
     */
    public Inventory getDefaultInventory(Long chatId) {
        Inventory inventory = new Inventory();
        inventory.setItems(new ArrayList<>());
        inventory.setVest(getDefaultVest(chatId));
        inventory.setPrimaryWeapon(getDefaultWeapon(chatId));
        inventory = saveOrUpdateInventory(inventory);
        return inventory;
    }

    /**
     * Saves or updates inventory to repository
     * @param inventory inventory to save or update
     * @return saved or updated inventory
     */
    public Inventory saveOrUpdateInventory(Inventory inventory) {
        val inventoryDocument = InventoryMapper.INSTANCE.mapToDocument(inventory);
        val savedInventoryDocument = inventoryRepository.save(inventoryDocument);
        return InventoryMapper.INSTANCE.mapToEntity(savedInventoryDocument);
    }

    /**
     * Passes data required for inventory menu message
     * @param chatId id of chat where message sent
     * @return true if message successfully sent
     */
    public boolean sendOrUpdateInventoryMessage(Long chatId) {
        val player = playerService.getPlayer(chatId);
        messageService.sendInventoryMessage(chatId, player.getInventory());
        return true;
    }

    /**
     * Equips item from inventory
     * @param chatId id of player's chat
     * @param itemId id of equipped item
     * @return true if item successfully equipped
     */
    public boolean equipItem(Long chatId, String itemId) {
        val player = playerService.getPlayer(chatId);
        val item = itemService.findItem(chatId, itemId);
        val inventory = player.getInventory();
        if (equipItem(inventory, item)) {
            messageService.sendInventoryItemMessage(chatId, item, CallbackType.INVENTORY, Optional.empty());
            return true;
        }
        return false;
    }

    /**
     * Un-equips item to inventory
     * @param chatId id of player's chat
     * @param itemId id of un-equipped item
     * @return true if item successfully un-equipped
     */
    public boolean unEquipItem(Long chatId, String itemId) {
        val player = playerService.getPlayer(chatId);
        val item= itemService.findItem(chatId, itemId);
        val inventory = player.getInventory();
        if ((inventory.isFull())) {
            //TODO implement prompt
            messageService.sendInventoryItemMessage(chatId, item, CallbackType.INVENTORY, Optional.empty());
            return false;
        }
        val result = unEquipItem(item, inventory);
        if (result) {
            player.removeItemEffects(item.getEffects());
        }
        messageService.sendInventoryMessage(chatId, inventory);
        return true;
    }

    /**
     * Sells item from inventory to merchant
     * @param chatId id of player's chat
     * @param itemId id of sold item
     * @return true if item successfully sold
     */
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
            messageService.sendMerchantSellMenuMessage(chatId, player);
            return true;
        }
        return false;
    }

    /**
     * Buys item from merchant and adds to player inventory
     * @param chatId id of player's chat
     * @param itemId id of bought item
     * @return true if item successfully bought
     */
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
        messageService.sendMerchantBuyMenuMessage(chatId, player.getGold(),
                ((Merchant)currentRoom.getRoomContent()).getItems());
        return true;
    }

    /**
     * Sends inventory info message
     * @param chatId id of chat where message sent
     * @param itemId id of item
     * @param inventoryType from what menu message info requested,
     *                      {@link CallbackType.INVENTORY} or {@link CallbackType.MERCHANT_SELL_MENU}
     * @param callbackType optional value, present if item equipped, permitted values:
     *                     {@link CallbackType.HEAD}, {@link CallbackType.VEST}, {@link CallbackType.GLOVES},
     *                     {@link CallbackType.BOOTS}, {@link CallbackType.LEFT_HAND}, {@link CallbackType.RIGHT_HAND}
     * @return
     */
    public boolean openInventoryItemInfo(Long chatId, String itemId, CallbackType inventoryType, Optional<CallbackType> callbackType) {
        val item = itemService.findItem(chatId, itemId);
        messageService.sendInventoryItemMessage(chatId, item, inventoryType,
                        Optional.ofNullable(MessageUtil.formatItemType(callbackType.orElse(null))));
        return true;
    }

    private Wearable getDefaultVest(Long chatId) {
        return itemService.getMostLightweightWearable(chatId, WearableType.VEST);
    }

    private Weapon getDefaultWeapon(Long chatId) {
        return itemService.getMostLightWeightMainWeapon(chatId);
    }

    private boolean equipItem(Inventory inventory, Item item) {
        if (isNull(item)) {
            return false;
        }
        return switch (item.getItemType()) {
            case WEAPON -> processWeaponEquip(inventory, (Weapon) item);
            case WEARABLE -> processWearableEquip(inventory, (Wearable) item);
            case USABLE -> processUsable(inventory);
        };
    }

    private boolean unEquipItem(Item item, Inventory inventory) {
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
        switch (wearable.getAttributes().getWearableType()) {
            case HELMET -> inventory.setHelmet(wearable);
            case VEST -> inventory.setVest(wearable);
            case GLOVES -> inventory.setGloves(wearable);
            case BOOTS -> inventory.setBoots(wearable);
        }
        inventory.getItems().remove(wearable);
        saveOrUpdateInventory(inventory);
        return true;
    }

    //TODO: add size constraint and write more test cases
    private boolean processWeaponEquip(Inventory inventory, Weapon weapon) {
        switch (weapon.getAttributes().getHandling()) {
            case SINGLE_HANDED -> {
                if (nonNull(inventory.getPrimaryWeapon())) {
                    if (nonNull(inventory.getSecondaryWeapon())) {
                        inventory.addItem(inventory.getSecondaryWeapon());
                    }
                    inventory.setSecondaryWeapon(weapon);
                } else {
                    inventory.setPrimaryWeapon(weapon);
                }
            }
            case TWO_HANDED -> {
                if (nonNull(inventory.getPrimaryWeapon())) {
                    inventory.addItem(inventory.getPrimaryWeapon());
                    inventory.setPrimaryWeapon(null);
                }
                if (nonNull(inventory.getSecondaryWeapon())) {
                    inventory.addItem(inventory.getSecondaryWeapon());
                    inventory.setSecondaryWeapon(null);
                }
                inventory.setPrimaryWeapon(weapon);
            }
        }
        inventory.getItems().remove(weapon);
        saveOrUpdateInventory(inventory);
        return true;
    }
}
