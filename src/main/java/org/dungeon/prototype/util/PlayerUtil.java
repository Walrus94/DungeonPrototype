package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import org.dungeon.prototype.model.inventory.ArmorSet;
import org.dungeon.prototype.model.inventory.WeaponSet;
import org.dungeon.prototype.model.inventory.Wearable;
import org.dungeon.prototype.model.inventory.items.Vest;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.player.Attribute;

import java.util.EnumMap;

import static org.dungeon.prototype.model.player.Attribute.*;

@UtilityClass
public class PlayerUtil {

    public static EnumMap<Attribute, Integer> initializeAttributes() {
        //TODO: consider different classes
        //TODO: move to properties
        EnumMap<Attribute, Integer> map = new EnumMap<>(Attribute.class);
        map.put(POWER, 5);
        map.put(STAMINA, 4);
        map.put(RECEPTION, 3);
        map.put(MAGIC, 1);
        map.put(LUCK, 2);
        return map;
    }
    public static Integer calculateAttack(WeaponSet weapon, EnumMap<Attribute, Integer> attributes) {
        return weapon.getWeapons().stream().mapToInt(Weapon::getAttack).sum() + attributes.get(POWER);
    }

    public static ArmorSet getDefaultArmorSet() {
        ArmorSet armorSet = new ArmorSet();
        Vest vest = new Vest();
        vest.setName("Leather vest");
        vest.setArmor(5);
        vest.setSellingPrice(0);
        vest.setBuyingPrice(10);
        armorSet.setVest(vest);
        return armorSet;
    }

    public static WeaponSet getDefaultWeaponSet() {
        WeaponSet weaponSet = new WeaponSet();
        weaponSet.addWeapon(new Weapon(Weapon.Type.SINGLE_HANDED, "Basic sword", 10, 15, 0));
        return weaponSet;
    }

    public static Integer calculateMaxDefense(ArmorSet armor) {
        return armor.getArmorItems().isEmpty() ? 0 : armor.getArmorItems().stream().mapToInt(Wearable::getArmor).sum();
    }
}
