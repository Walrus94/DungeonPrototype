package org.dungeon.prototype.model.inventory;

import lombok.Data;

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
        if (items.size() == maxItems) {
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
}
