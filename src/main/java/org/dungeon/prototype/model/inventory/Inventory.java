package org.dungeon.prototype.model.inventory;

import lombok.Data;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    public void remove(Item item) {
        if (items.remove(item)) {
            return;
        }
        if (item instanceof Wearable && armorSet.getArmorItems().contains(item)) {
            switch (((Wearable)item).getAttributes().getWearableType()) {
                case HELMET -> armorSet.setHelmet(null);
                case VEST -> armorSet.setVest(null);
                case GLOVES -> armorSet.setGloves(null);
                case BOOTS -> armorSet.setBoots(null);
            }
        }
        if (item instanceof Weapon) {
            if (item.equals(weaponSet.getSecondaryWeapon())) {
                weaponSet.setSecondaryWeapon(null);
            }
            if (item.equals(weaponSet.getPrimaryWeapon())) {
                weaponSet.setPrimaryWeapon(null);
            }
        }
    }

    public boolean isFull() {
        return maxItems.equals(items.size());
    }
}
