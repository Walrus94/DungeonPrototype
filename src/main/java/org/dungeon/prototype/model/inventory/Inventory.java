package org.dungeon.prototype.model.inventory;

import lombok.Data;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dungeon.prototype.model.inventory.attributes.weapon.Handling.SINGLE_HANDED;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Handling.TWO_HANDED;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Size.LARGE;

@Data
public class Inventory {
    private String id;
    private Integer maxItems;
    private List<Item> items;
    private Wearable helmet;
    private Wearable vest;
    private Wearable gloves;
    private Wearable boots;
    private Weapon primaryWeapon;
    private Weapon secondaryWeapon;

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

    public boolean addWeapon(Weapon weapon) {
        switch (weapon.getAttributes().getHandling()) {
            case TWO_HANDED -> {
                return !Objects.nonNull(primaryWeapon) && !Objects.nonNull(secondaryWeapon);
            }
            case SINGLE_HANDED -> {
                if (Objects.nonNull(primaryWeapon) && TWO_HANDED.equals(primaryWeapon.getAttributes().getHandling())) {
                    return false;
                } else if (Objects.isNull(primaryWeapon)) {
                    primaryWeapon = weapon;
                } else if (primaryWeapon.getAttributes().getHandling().equals(SINGLE_HANDED) && !primaryWeapon.getAttributes().getSize().equals(LARGE)) {
                    secondaryWeapon = weapon;
                }
            }
        }
        return false;
    }

    public Optional<Item> unEquip(Item item) {
        if (items.contains(item)) {
            return Optional.empty();
        }
        if (item instanceof Wearable && getArmorItems().contains(item)) {
            switch (((Wearable)item).getAttributes().getWearableType()) {
                case HELMET -> {
                    helmet = null;
                    return Optional.of(item);
                }
                case VEST -> {
                    vest = null;
                    return Optional.of(item);
                }
                case GLOVES -> {
                    gloves = null;
                    return Optional.of(item);
                }
                case BOOTS -> {
                    boots = null;
                    return Optional.of(item);
                }
            }
            return Optional.empty();
        }
        if (item instanceof Weapon) {
            if (item.equals(secondaryWeapon)) {
                secondaryWeapon = null;
                return Optional.of(item);
            }
            if (item.equals(primaryWeapon)) {
                primaryWeapon = null;
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
        if (item instanceof Wearable && getArmorItems().contains(item)) {
            switch (((Wearable)item).getAttributes().getWearableType()) {
                case HELMET -> {
                    helmet = null;
                    return true;
                }
                case VEST -> {
                    vest = null;
                    return true;
                }
                case GLOVES -> {
                    gloves = null;
                    return true;
                }
                case BOOTS -> {
                    boots = null;
                    return true;
                }
            }
            return false;
        }
        if (item instanceof Weapon) {
            if (item.equals(secondaryWeapon)) {
                secondaryWeapon = null;
                return true;
            }
            if (item.equals(primaryWeapon)) {
                primaryWeapon = null;
                return true;
            }
            return false;
        }
        return false;
    }

    public List<Wearable> getArmorItems() {
        var result = new ArrayList<Wearable>();
        if (helmet != null) {
            result.add(helmet);
        }
        if (vest != null) {
            result.add(vest);
        }
        if (gloves != null) {
            result.add(gloves);
        }
        if (boots != null) {
            result.add(boots);
        }
        return result;
    }

    public List<Weapon> getWeapons() {
        return Stream.of(primaryWeapon, secondaryWeapon)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
