package org.dungeon.prototype.service.inventory;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.async.AsyncJobHandler;
import org.dungeon.prototype.async.TaskType;
import org.dungeon.prototype.exception.DungeonPrototypeException;
import org.dungeon.prototype.exception.RestrictedOperationException;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.repository.mongo.InventoryRepository;
import org.dungeon.prototype.repository.mongo.converters.mapstruct.InventoryMapper;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.util.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.dungeon.prototype.properties.CallbackType.MERCHANT_BUY_MENU;

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
    private EffectService effectService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private AsyncJobHandler asyncJobHandler;

    /**
     * Builds default inventory for start of the game
     * @param chatId id of player's chat
     * @return inventory, containing primary weapon and vest
     */
    public Inventory getDefaultInventory(Long chatId) {
        log.info("Setting default inventory");
        messageService.sendPlayerGeneratingInfoMessage(chatId);
        try {
            return (Inventory) asyncJobHandler.submitTask(() -> {
                Inventory inventory = new Inventory();
                inventory.setItems(new ArrayList<>());
                inventory.setVest(getDefaultVest(chatId));
                inventory.setPrimaryWeapon(getDefaultWeapon(chatId));
                inventory = saveOrUpdateInventory(inventory);
                return inventory;
            }, TaskType.GET_DEFAULT_INVENTORY, chatId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DungeonPrototypeException(e.getMessage());
        }
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
     */
    public void sendInventoryMessage(Long chatId, Player player) {
        messageService.sendInventoryMessage(chatId, player.getInventory());
    }

    /**
     * Equips item from inventory
     * @param chatId id of player's chat
     * @param itemId id of equipped item
     */
    public void equipItem(Long chatId, String itemId) {
        var player = playerService.getPlayer(chatId);
        val item = itemService.findItem(chatId, itemId);
        val inventory = player.getInventory();
        if (inventory.equipItem(item)) {
            player.addEffects(new ArrayList<>(item.getEffects()));
            player = effectService.updateArmorEffect(player);
            playerService.updatePlayer(player);
            saveOrUpdateInventory(inventory);
            messageService.sendInventoryMessage(chatId, inventory);
        }
    }

    /**
     * Un-equips item to inventory
     * @param chatId id of player's chat
     * @param itemId id of un-equipped item
     */
    public void unEquipItem(Long chatId, String itemId) {
        val player = playerService.getPlayer(chatId);
        val item= itemService.findItem(chatId, itemId);
        val inventory = player.getInventory();
        if ((inventory.isFull())) {
            //TODO: implement prompt
            messageService.sendInventoryItemMessage(chatId, item, CallbackType.INVENTORY, Optional.empty());
            return;
        }
        if (inventory.unEquip(item) && (item.getEffects().isEmpty() || player.removeEffects(item.getEffects()))) {
            effectService.updateArmorEffect(player);
            saveOrUpdateInventory(inventory);
            messageService.sendInventoryMessage(chatId, inventory);
        }
    }

    /**
     * Sells item from inventory to merchant
     * @param chatId id of player's chat
     * @param itemId id of sold item
     */
    public void sellItem(Long chatId, String itemId) {
        var player = playerService.getPlayer(chatId);
        val item = itemService.findItem(chatId, itemId);
        val inventory = player.getInventory();
        val isEquipped = inventory.isEquipped(item);
        if (inventory.removeItem(item)) {
            player.addGold(item.getSellingPrice());
            saveOrUpdateInventory(inventory);
            if (isEquipped && !item.getEffects().isEmpty()) {
                 player.removeEffects(item.getEffects());
                 player = effectService.updateArmorEffect(player);
            }
            playerService.updatePlayer(player);
            messageService.sendMerchantSellMenuMessage(chatId, player);
        }
    }

    /**
     * Buys item from merchant and adds to player inventory
     * @param chatId id of player's chat
     * @param itemId id of bought item
     */
    public void buyItem(Long chatId, String itemId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (player.getInventory().isFull()) {
            throw new RestrictedOperationException(chatId, "buy item", "Inventory is full!", MERCHANT_BUY_MENU);
        }
        val item = itemService.findItem(chatId, itemId);
        if (player.getGold() < item.getBuyingPrice()) {
            throw new RestrictedOperationException(chatId, "buy item", "Not enough money!", MERCHANT_BUY_MENU);
        }
        player.getInventory().addItem(item);
        ((Merchant) currentRoom.getRoomContent()).getItems().remove(item);
        player.removeGold(item.getSellingPrice());
        playerService.updatePlayer(player);
        roomService.saveOrUpdateRoom(currentRoom);
        saveOrUpdateInventory(player.getInventory());
        messageService.sendMerchantBuyMenuMessage(chatId, player.getGold(),
                ((Merchant)currentRoom.getRoomContent()).getItems());
    }

    /**
     * Sends inventory info message
     * @param chatId id of chat where message sent
     * @param itemId id of item
     * @param inventoryType from what menu message info requested,
     *                      {@link CallbackType#INVENTORY} or {@link CallbackType#MERCHANT_SELL_MENU}
     * @param callbackType optional value, present if item equipped, permitted values:
     *                     {@link CallbackType#HEAD}, {@link CallbackType#VEST}, {@link CallbackType#GLOVES},
     *                     {@link CallbackType#BOOTS}, {@link CallbackType#LEFT_HAND}, {@link CallbackType#RIGHT_HAND}
     */
    public void openInventoryItemInfo(Long chatId, String itemId, CallbackType inventoryType, Optional<CallbackType> callbackType) {
        val item = itemService.findItem(chatId, itemId);
        messageService.sendInventoryItemMessage(chatId, item, inventoryType,
                        Optional.ofNullable(MessageUtil.formatItemType(callbackType.orElse(null))));
    }

    public void sharpenWeapon(Long chatId) {
        //TODO: implement
    }

    private Wearable getDefaultVest(Long chatId) {
        return itemService.getMostLightweightWearable(chatId, WearableType.VEST);
    }

    private Weapon getDefaultWeapon(Long chatId) {
        return itemService.getMostLightWeightMainWeapon(chatId);
    }
}
