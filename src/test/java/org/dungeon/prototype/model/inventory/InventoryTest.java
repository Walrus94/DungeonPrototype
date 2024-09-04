package org.dungeon.prototype.model.inventory;

import lombok.val;
import org.dungeon.prototype.BaseUnitTest;
import org.dungeon.prototype.model.inventory.attributes.weapon.Handling;
import org.dungeon.prototype.model.inventory.attributes.weapon.Size;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponType;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.junit.Ignore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest extends BaseUnitTest {

    @Test
    @DisplayName("Successfully adds item to inventory")
    void addItem() {
        Inventory inventory = new Inventory();
        inventory.setItems(new ArrayList<>());

        val actualResult = inventory.addItem(new Weapon());

        assertEquals(1, inventory.getItems().size());
        assertTrue(actualResult);
    }

    @Test
    @DisplayName("Fails adding item to full inventory")
    void addItem_failedInventoryFull() {
        Inventory inventory = new Inventory();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < inventory.getMaxItems(); i++) {
            items.add(new Wearable());
        }
        inventory.setItems(items);

        val actualResult = inventory.addItem(new Weapon());

        assertEquals(inventory.getMaxItems(), inventory.getItems().size());
        assertFalse(actualResult);
    }

    @Test
    @DisplayName("Successfully adds items to inventory")
    void addItems() {
        Inventory inventory = new Inventory();
        inventory.setItems(new ArrayList<>());
        List<Item> itemsToAdd = new ArrayList<>();
        itemsToAdd.add(new Wearable());
        itemsToAdd.add(new Weapon());
        itemsToAdd.add(new Weapon());

        val actualResult = inventory.addItems(itemsToAdd);

        assertEquals(itemsToAdd.size(), inventory.getItems().size());
        assertTrue(actualResult);
    }

    @Test
    @DisplayName("Fails to add items to inventory due to lack of space")
    void addItems_failedNotEnoughSpace() {
        Inventory inventory = new Inventory();
        int itemsSize = 7;
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < itemsSize; i++) {
            items.add(new Wearable());
        }
        inventory.setItems(items);
        List<Item> itemsToAdd = new ArrayList<>();
        itemsToAdd.add(new Wearable());
        itemsToAdd.add(new Weapon());
        itemsToAdd.add(new Weapon());

        val actualResult = inventory.addItems(itemsToAdd);

        assertEquals(items.size(), inventory.getItems().size());
        assertFalse(actualResult);
    }

    @Test
    @DisplayName("Successfully equips wearable item")
    void equipWearableItem() {
        Inventory inventory = new Inventory();
        Wearable vest = new Wearable();
        WearableAttributes wearableAttributes = new WearableAttributes();
        wearableAttributes.setWearableType(WearableType.VEST);
        vest.setAttributes(wearableAttributes);
        List<Item> items = new ArrayList<>();
        items.add(vest);
        inventory.setItems(items);

        val actualResult = inventory.equipItem(vest);

        assertEquals(vest, inventory.getVest());
        assertTrue(actualResult);

    }

    @Test
    @DisplayName("Successfully equips wearable item with full inventory, adding replaced wearable to inventory")
    void equipWearableItem_swapWithFullInventory() {
        Inventory inventory = new Inventory();
        Wearable oldVest = new Wearable();
        WearableAttributes oldWearableAttributes = new WearableAttributes();
        oldWearableAttributes.setWearableType(WearableType.VEST);
        oldVest.setAttributes(oldWearableAttributes);
        inventory.setVest(oldVest);
        Wearable vest = new Wearable();
        WearableAttributes wearableAttributes = new WearableAttributes();
        wearableAttributes.setWearableType(WearableType.VEST);
        vest.setAttributes(wearableAttributes);
        List<Item> items = new ArrayList<>();
        for (int i = 1; i < inventory.getMaxItems(); i++) {
            items.add(new Weapon());
        }
        items.add(vest);
        inventory.setItems(items);

        val actualResult = inventory.equipItem(vest);

        assertTrue(actualResult);
        assertTrue(inventory.getItems().contains(oldVest));
        assertEquals(inventory.getMaxItems(), inventory.getItems().size());
        assertEquals(vest, inventory.getVest());
    }

    @Test
    @DisplayName("Successfully equips two-handed weapon")
    void equipTwoHandedWeapon() {
        Inventory inventory = new Inventory();
        Weapon primaryWeapon = new Weapon();
        Weapon secondaryWeapon = new Weapon();
        inventory.setPrimaryWeapon(primaryWeapon);
        inventory.setSecondaryWeapon(secondaryWeapon);
        Weapon newWeapon = new Weapon();
        WeaponAttributes attributes = new WeaponAttributes();
        attributes.setHandling(Handling.TWO_HANDED);
        newWeapon.setAttributes(attributes);
        List<Item> items = new ArrayList<>();
        items.add(newWeapon);
        inventory.setItems(items);

        val actualResult = inventory.equipItem(newWeapon);

        assertTrue(actualResult);
        assertTrue(inventory.getItems().contains(primaryWeapon));
        assertTrue(inventory.getItems().contains(secondaryWeapon));

        assertEquals(newWeapon, inventory.getPrimaryWeapon());
        assertNull(inventory.getSecondaryWeapon());
    }

    @Test
    @DisplayName("Successfully equips secondary weapon")
    void equipSecondaryWeapon() {
        Inventory inventory = new Inventory();
        Weapon primaryWeapon = new Weapon();
        WeaponAttributes primaryWeaponAttributes = new WeaponAttributes();
        primaryWeaponAttributes.setWeaponType(WeaponType.SWORD);
        primaryWeaponAttributes.setSize(Size.LARGE);
        primaryWeaponAttributes.setHandling(Handling.SINGLE_HANDED);
        primaryWeapon.setAttributes(primaryWeaponAttributes);
        inventory.setPrimaryWeapon(primaryWeapon);

        Weapon secondaryWeapon = new Weapon();
        WeaponAttributes secondaryWeaponAttributes = new WeaponAttributes();
        secondaryWeaponAttributes.setWeaponType(WeaponType.DAGGER);
        secondaryWeaponAttributes.setSize(Size.SMALL);
        secondaryWeaponAttributes.setHandling(Handling.SINGLE_HANDED);
        secondaryWeapon.setAttributes(secondaryWeaponAttributes);

        List<Item> items = new ArrayList<>();
        items.add(secondaryWeapon);
        inventory.setItems(items);

        val actualResult = inventory.equipItem(secondaryWeapon);

        assertTrue(actualResult);
        assertEquals(inventory.getSecondaryWeapon(), secondaryWeapon);
    }

    @Test
    @DisplayName("Successfully equips secondary weapon instead of existing one")
    void equipSecondaryWeaponReplacingExisting() {
        Inventory inventory = new Inventory();
        Weapon primaryWeapon = new Weapon();
        WeaponAttributes primaryWeaponAttributes = new WeaponAttributes();
        primaryWeaponAttributes.setWeaponType(WeaponType.SWORD);
        primaryWeaponAttributes.setSize(Size.MEDIUM);
        primaryWeaponAttributes.setHandling(Handling.SINGLE_HANDED);
        primaryWeapon.setAttributes(primaryWeaponAttributes);
        inventory.setPrimaryWeapon(primaryWeapon);

        Weapon secondaryWeapon = new Weapon();
        WeaponAttributes secondaryWeaponAttributes = new WeaponAttributes();
        secondaryWeaponAttributes.setWeaponType(WeaponType.DAGGER);
        secondaryWeaponAttributes.setSize(Size.SMALL);
        secondaryWeaponAttributes.setHandling(Handling.SINGLE_HANDED);
        secondaryWeapon.setAttributes(secondaryWeaponAttributes);
        inventory.setSecondaryWeapon(secondaryWeapon);

        Weapon newWeapon = new Weapon();
        WeaponAttributes newWeaponAttributes = new WeaponAttributes();
        newWeaponAttributes.setWeaponType(WeaponType.SWORD);
        newWeaponAttributes.setSize(Size.MEDIUM);
        newWeaponAttributes.setHandling(Handling.SINGLE_HANDED);
        newWeapon.setAttributes(newWeaponAttributes);

        List<Item> items = new ArrayList<>();
        items.add(newWeapon);
        inventory.setItems(items);

        val actualResult = inventory.equipItem(newWeapon);

        assertTrue(actualResult);
        assertEquals(newWeapon, inventory.getSecondaryWeapon());
        assertEquals(primaryWeapon, inventory.getPrimaryWeapon());
        assertTrue(inventory.getItems().contains(secondaryWeapon));
    }

    @Test
    @DisplayName("Successfully equips large weapon instead of existing primary medium one")
    void equipsLargePrimaryWeaponInsteadOfMedium() {
        Inventory inventory = new Inventory();
        Weapon primaryWeapon = new Weapon();
        WeaponAttributes primaryWeaponAttributes = new WeaponAttributes();
        primaryWeaponAttributes.setWeaponType(WeaponType.SWORD);
        primaryWeaponAttributes.setSize(Size.MEDIUM);
        primaryWeaponAttributes.setHandling(Handling.SINGLE_HANDED);
        primaryWeapon.setAttributes(primaryWeaponAttributes);
        inventory.setPrimaryWeapon(primaryWeapon);

        Weapon secondaryWeapon = new Weapon();
        WeaponAttributes secondaryWeaponAttributes = new WeaponAttributes();
        secondaryWeaponAttributes.setWeaponType(WeaponType.DAGGER);
        secondaryWeaponAttributes.setSize(Size.SMALL);
        secondaryWeaponAttributes.setHandling(Handling.SINGLE_HANDED);
        secondaryWeapon.setAttributes(secondaryWeaponAttributes);
        inventory.setSecondaryWeapon(secondaryWeapon);

        Weapon newWeapon = new Weapon();
        WeaponAttributes newWeaponAttributes = new WeaponAttributes();
        newWeaponAttributes.setWeaponType(WeaponType.SWORD);
        newWeaponAttributes.setSize(Size.LARGE);
        newWeaponAttributes.setHandling(Handling.SINGLE_HANDED);
        newWeapon.setAttributes(newWeaponAttributes);

        List<Item> items = new ArrayList<>();
        items.add(newWeapon);
        inventory.setItems(items);

        val actualResult = inventory.equipItem(newWeapon);

        assertTrue(actualResult);
        assertEquals(newWeapon, inventory.getPrimaryWeapon());
        assertEquals(secondaryWeapon, inventory.getSecondaryWeapon());
        assertTrue(inventory.getItems().contains(primaryWeapon));
    }

    @Test
    @DisplayName("Fails to equip two-handed weapon instead of two single-handed due to lack of inventory space")
    void equip_failedTwoHandedLackOfSpace() {
        Inventory inventory = new Inventory();
        Weapon primaryWeapon = new Weapon();
        WeaponAttributes primaryWeaponAttributes = new WeaponAttributes();
        primaryWeaponAttributes.setWeaponType(WeaponType.SWORD);
        primaryWeaponAttributes.setSize(Size.MEDIUM);
        primaryWeaponAttributes.setHandling(Handling.SINGLE_HANDED);
        primaryWeapon.setAttributes(primaryWeaponAttributes);
        inventory.setPrimaryWeapon(primaryWeapon);

        Weapon secondaryWeapon = new Weapon();
        WeaponAttributes secondaryWeaponAttributes = new WeaponAttributes();
        secondaryWeaponAttributes.setWeaponType(WeaponType.DAGGER);
        secondaryWeaponAttributes.setSize(Size.SMALL);
        secondaryWeaponAttributes.setHandling(Handling.SINGLE_HANDED);
        secondaryWeapon.setAttributes(secondaryWeaponAttributes);
        inventory.setSecondaryWeapon(secondaryWeapon);

        Weapon newWeapon = new Weapon();
        WeaponAttributes newWeaponAttributes = new WeaponAttributes();
        newWeaponAttributes.setWeaponType(WeaponType.SWORD);
        newWeaponAttributes.setSize(Size.LARGE);
        newWeaponAttributes.setHandling(Handling.TWO_HANDED);
        newWeapon.setAttributes(newWeaponAttributes);

        List<Item> items = new ArrayList<>();
        items.add(newWeapon);
        for (int i = 1; i < inventory.getMaxItems(); i++) {
            items.add(new Wearable());
        }
        inventory.setItems(items);

        val actualResult = inventory.equipItem(newWeapon);

        assertFalse(actualResult);
        assertEquals(primaryWeapon, inventory.getPrimaryWeapon());
        assertEquals(secondaryWeapon, inventory.getSecondaryWeapon());
        assertTrue(inventory.getItems().contains(newWeapon));
    }

    @Test
    @DisplayName("Successfully un-equips item and put it to inventory items")
    void unEquip() {
        Inventory inventory = new Inventory();
        Wearable vest = new Wearable();
        WearableAttributes attributes = new WearableAttributes();
        attributes.setWearableType(WearableType.VEST);
        vest.setAttributes(attributes);
        inventory.setVest(vest);

        val actualResult = inventory.unEquip(vest);

        assertTrue(actualResult);
        assertNull(inventory.getVest());
        assertTrue(inventory.getItems().contains(vest));
    }

    @Test
    @DisplayName("Fails to un-equip item due to full inventory items")
    void unEquip_failedFullInventory() {
        Inventory inventory = new Inventory();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < inventory.getMaxItems(); i++) {
            items.add(new Weapon());
        }
        inventory.setItems(items);
        Wearable vest = new Wearable();
        WearableAttributes attributes = new WearableAttributes();
        attributes.setWearableType(WearableType.VEST);
        vest.setAttributes(attributes);
        inventory.setVest(vest);

        val actualResult = inventory.unEquip(vest);

        assertFalse(actualResult);
        assertEquals(vest, inventory.getVest());
        assertFalse(inventory.getItems().contains(vest));
    }

    @Test
    @DisplayName("Successfully returns valid check result for full inventory")
    void isFullPositive() {
        Inventory inventory = new Inventory();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < inventory.getMaxItems(); i++) {
            items.add(new Weapon());
        }
        inventory.setItems(items);

        val actualResult = inventory.isFull();

        assertTrue(actualResult);
    }

    @Test
    @DisplayName("Successfully returns valid check result for inventory with empty space")
    void isFullNegative() {
        Inventory inventory = new Inventory();
        List<Item> items = new ArrayList<>();
        for (int i = 1; i < inventory.getMaxItems(); i++) {
            items.add(new Weapon());
        }
        inventory.setItems(items);

        val actualResult = inventory.isFull();

        assertFalse(actualResult);
    }

    @Test
    @DisplayName("Successfully removes item from inventory")
    void removeItem() {
        Inventory inventory = new Inventory();
        List<Item> items = new ArrayList<>();
        Weapon weapon = new Weapon();
        items.add(weapon);
        inventory.setItems(items);

        val actualResult = inventory.removeItem(weapon);

        assertTrue(actualResult);
        assertFalse(inventory.getItems().contains(weapon));
    }

    @Test
    @Ignore
    void getArmorItems() {
    }

    @Test
    @Ignore
    void getWeapons() {
    }

    @Test
    @Ignore
    void calculateMaxDefense() {
    }

    @Test
    @Ignore
    void clear() {
    }

    @Test
    @Ignore
    void isEquipped() {
    }
}