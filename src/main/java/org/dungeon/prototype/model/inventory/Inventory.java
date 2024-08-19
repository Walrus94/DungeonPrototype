package org.dungeon.prototype.model.inventory;

import lombok.Data;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Data
public class Inventory {
    private String id;
    private Integer maxItems;
    private List<Item> items;
    private ArmorSet armorSet;
    private WeaponSet weaponSet;

    public Inventory() {
        items = new ArrayList<>();
        maxItems = 9; //TODO: consider configuring
    }

    public boolean addItem(Item item) {
        if (isFull()) {
            return false;
        } else {
            return items.add(item);
        }
    }

    public boolean addItems(Collection<Item> items) {
        if (maxItems - this.items.size() < items.size()) {
            return false;
        } else {
            return this.items.addAll(items);
        }
    }

    public Optional<Item> unEquip(Item item) {
        if (items.remove(item)) {
            return Optional.empty();
        }
        if (item instanceof Wearable && armorSet.getArmorItems().contains(item)) {
            switch (((Wearable)item).getAttributes().getWearableType()) {
                case HELMET -> {
                    armorSet.setHelmet(null);
                    return Optional.of(item);
                }
                case VEST -> {
                    armorSet.setVest(null);
                    return Optional.of(item);
                }
                case GLOVES -> {
                    armorSet.setGloves(null);
                    return Optional.of(item);
                }
                case BOOTS -> {
                    armorSet.setBoots(null);
                    return Optional.of(item);
                }
            }
            return Optional.empty();
        }
        if (item instanceof Weapon) {
            if (item.equals(weaponSet.getSecondaryWeapon())) {
                weaponSet.setSecondaryWeapon(null);
                return Optional.of(item);
            }
            if (item.equals(weaponSet.getPrimaryWeapon())) {
                weaponSet.setPrimaryWeapon(null);
                return Optional.of(item);
            }
            return Optional.empty();
        }
        return Optional.empty();
    }

    public boolean isFull() {
        return maxItems.equals(items.size());
    }

    public boolean removeItem(Item item) {
        if (items.remove(item)) {
            return true;
        }
        if (item instanceof Wearable && armorSet.getArmorItems().contains(item)) {
            switch (((Wearable)item).getAttributes().getWearableType()) {
                case HELMET -> {
                    armorSet.setHelmet(null);
                    return true;
                }
                case VEST -> {
                    armorSet.setVest(null);
                    return true;
                }
                case GLOVES -> {
                    armorSet.setGloves(null);
                    return true;
                }
                case BOOTS -> {
                    armorSet.setBoots(null);
                    return true;
                }
            }
            return false;
        }
        if (item instanceof Weapon) {
            if (item.equals(weaponSet.getSecondaryWeapon())) {
                weaponSet.setSecondaryWeapon(null);
                return true;
            }
            if (item.equals(weaponSet.getPrimaryWeapon())) {
                weaponSet.setPrimaryWeapon(null);
                return true;
            }
            return false;
        }
        return false;
    }
}
