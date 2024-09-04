package org.dungeon.prototype.model.inventory;

import lombok.Data;
import lombok.val;
import org.dungeon.prototype.model.inventory.attributes.weapon.Size;
import org.dungeon.prototype.model.inventory.items.Usable;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Size.LARGE;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Size.SMALL;

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
        maxItems = 9; //TODO: consider configuring
        items = new ArrayList<>(9);
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

    public boolean equipItem(Item item) {
        if (isNull(item) || !items.contains(item)) {
            return false;
        }
        if (items.remove(item)) {
            if (switch (item.getItemType()) {
                case WEAPON -> processWeaponEquip((Weapon) item);
                case WEARABLE -> processWearableEquip((Wearable) item);
                case USABLE -> processUsable((Usable) item);
            }) {
                return true;
            } else {
                addItem(item);
                return false;
            }
        } else {
            return false;
        }
    }

    public Boolean unEquip(Item item) {
        if (item instanceof Wearable && getArmorItems().contains(item)) {
            switch (((Wearable)item).getAttributes().getWearableType()) {
                case HELMET -> {
                    if (item.equals(helmet) && addItem(item)) {
                        helmet = null;
                        return true;
                    } else {
                        return false;
                    }
                }
                case VEST -> {
                    if (item.equals(vest) && addItem(item)) {
                        vest = null;
                        return true;
                    } else {
                        return false;
                    }
                }
                case GLOVES -> {
                    if (item.equals(gloves) && addItem(item)) {
                        gloves = null;
                        return true;
                    } else {
                        return false;
                    }
                }
                case BOOTS -> {
                    if (item.equals(boots) && addItem(item)) {
                        boots = null;
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }
        if (item instanceof Weapon) {
            if (item.equals(secondaryWeapon) && addItem(item)) {
                secondaryWeapon = null;
                return true;
            }
            if (item.equals(primaryWeapon) && addItem(item)) {
                primaryWeapon = null;
                return true;
            }
            return false;
        }
        return false;
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

    public Integer calculateMaxDefense() {
        return getArmorItems().stream().filter(Objects::nonNull).mapToInt(Wearable::getArmor).sum();
    }

    public void clear() {
        primaryWeapon = null;
        secondaryWeapon = null;
        helmet = null;
        vest = null;
        gloves = null;
        boots = null;
    }

    public boolean isEquipped(Item item) {
        return getArmorItems().contains(item) || getWeapons().contains(item);
    }

    private boolean processUsable(Usable usable) {
        //TODO: consider mocking (since considering using Usables directly from inventory list
        return true;
    }

    private Boolean processWearableEquip(Wearable wearable) {
        return switch (wearable.getAttributes().getWearableType()) {
            case HELMET -> {
                if (nonNull(helmet)) {
                    if (addItem(helmet)) {
                        helmet = wearable;
                        yield true;
                    } else {
                        yield false;
                    }
                } else {
                    helmet = wearable;
                    yield true;
                }
            }
            case VEST -> {
                if (nonNull(vest)) {
                    if (addItem(vest)) {
                        vest = wearable;
                        yield true;
                    } else {
                        yield false;
                    }
                } else {
                    vest = wearable;
                    yield true;
                }
            }
            case GLOVES -> {
                if (nonNull(gloves)) {
                    if (addItem(gloves)) {
                        gloves = wearable;
                        yield true;
                    } else {
                        yield false;
                    }
                } else {
                    gloves = wearable;
                    yield true;
                }
            }
            case BOOTS -> {
                if (nonNull(boots)) {
                    if (addItem(boots)) {
                        boots = wearable;
                        yield true;
                    } else {
                        yield false;
                    }
                } else {
                    boots = wearable;
                    yield true;
                }
            }
        };
    }

    private boolean processWeaponEquip(Weapon weapon) {
        if (isNull(primaryWeapon)) {
            primaryWeapon = weapon;
            return true;
        }
        switch (weapon.getAttributes().getHandling()) {
            case SINGLE_HANDED -> {
                if (isPermittedSizeSummary(primaryWeapon, weapon)) {
                    if (primaryWeapon.getAttributes().getSize().compareTo(weapon.getAttributes().getSize()) >= 0) {
                        if (nonNull(secondaryWeapon)) {
                            if (addItem(secondaryWeapon)) {
                                secondaryWeapon = weapon;
                                return true;
                            } else {
                                return false;
                            }
                        } else {
                            secondaryWeapon = weapon;
                            return true;
                        }
                    } else {
                        if (addItem(primaryWeapon)) {
                            if (nonNull(secondaryWeapon) && !isPermittedSizeSummary(secondaryWeapon, weapon)) {
                                if (addItem(secondaryWeapon)) {
                                    secondaryWeapon = null;
                                    primaryWeapon = weapon;
                                    return true;
                                } else {
                                    items.remove(primaryWeapon);
                                    return false;
                                }
                            }
                            primaryWeapon = weapon;
                            return true;
                        } else {
                            return false;
                        }
                    }
                } else {
                    if (nonNull(secondaryWeapon)) {
                        if (!isPermittedSizeSummary(secondaryWeapon, weapon)) {
                            if (addItem(secondaryWeapon) && addItem(primaryWeapon)) {
                                primaryWeapon = weapon;
                                return true;
                            } else {
                                return false;
                            }
                        } else {
                            if (addItem(primaryWeapon)) {
                                primaryWeapon = weapon;
                                return true;
                            }
                        }
                    } else {
                        if (addItem(primaryWeapon)) {
                            primaryWeapon = weapon;
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            }
            case TWO_HANDED -> {
                val prevPrimaryWeapon = primaryWeapon;
                if (nonNull(primaryWeapon)) {
                    if (addItem(primaryWeapon)) {
                        primaryWeapon = null;
                    } else {
                        return false;
                    }
                }
                if (nonNull(secondaryWeapon)) {
                    if (addItem(secondaryWeapon)) {
                        secondaryWeapon = null;
                    } else {
                        items.remove(prevPrimaryWeapon);
                        primaryWeapon = prevPrimaryWeapon;
                        return false;
                    }
                }
               primaryWeapon = weapon;
            }
        }
        return true;
    }

    private boolean isPermittedSizeSummary(Weapon first, Weapon second) {
        return isPermittedSizeSummary(first.getAttributes().getSize(), second.getAttributes().getSize());
    }

    private boolean isPermittedSizeSummary(Size first, Size second) {
        if (first.compareTo(second) > 0) {
            if (first.equals(LARGE)) {
                return second.equals(SMALL);
            } else {
                return true;
            }
        } else {
            if (second.equals(LARGE)) {
                return first.equals(SMALL);
            } else {
                return true;
            }
        }
    }
}
