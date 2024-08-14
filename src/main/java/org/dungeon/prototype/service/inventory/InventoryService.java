package org.dungeon.prototype.service.inventory;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.inventory.ArmorSet;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.WeaponSet;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.repository.ArmorSetRepository;
import org.dungeon.prototype.repository.InventoryRepository;
import org.dungeon.prototype.repository.WeaponSetRepository;
import org.dungeon.prototype.repository.converters.mapstruct.ArmorSetMapper;
import org.dungeon.prototype.repository.converters.mapstruct.InventoryMapper;
import org.dungeon.prototype.repository.converters.mapstruct.WeaponSetMapper;
import org.dungeon.prototype.service.item.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Service
public class InventoryService {
    @Autowired
    ItemService itemService;
    @Autowired
    InventoryRepository inventoryRepository;

    @Autowired
    ArmorSetRepository armorSetRepository;

    @Autowired
    WeaponSetRepository weaponSetRepository;

    public Inventory getDefaultInventory(Long chatId) {
        Inventory inventory = new Inventory();
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
        if (item instanceof Wearable wearable) {
            val armorSet = inventory.getArmorSet();
            switch (wearable.getAttributes().getWearableType()) {
                case HELMET -> armorSet.setHelmet(null);
                case VEST -> armorSet.setVest(null);
                case GLOVES -> armorSet.setGloves(null);
                case BOOTS -> armorSet.setBoots(null);
            }
            saveOrUpdateArmorSet(armorSet);
        } else if (item instanceof Weapon weapon) {
            val weaponSet = inventory.getWeaponSet();
            if (weapon.equals(weaponSet.getSecondaryWeapon())) {
                weaponSet.setSecondaryWeapon(null);
            } else if (weapon.equals(weaponSet.getPrimaryWeapon())) {
                if (nonNull(weaponSet.getSecondaryWeapon())) {
                    weaponSet.setPrimaryWeapon(weaponSet.getSecondaryWeapon());
                    weaponSet.setSecondaryWeapon(null);
                } else {
                    weaponSet.setPrimaryWeapon(null);
                }
            }
        }
        inventory.addItem(item);
        saveOrUpdateInventory(inventory);
        return true;
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
