package org.dungeon.prototype.model.inventory;

import lombok.Data;
import org.dungeon.prototype.model.inventory.items.Weapon;

import java.util.ArrayList;
import java.util.List;

@Data
public class WeaponSet {

    public WeaponSet() {
        this.weapons = new ArrayList<>(2);
    }
    private List<Weapon> weapons;

    public boolean addWeapon(Weapon weapon) {
        switch (weapon.getType()) {
            case SINGLE_HANDED, TWO_HANDED -> {
                if (weapons.size() > 0) {
                    return false;
                }
                weapons.add(weapon);
                return true;
            }
            case ADDITIONAL -> {
                if (weapons.size() == 1 && Weapon.Type.SINGLE_HANDED.equals(weapons.getFirst().getType())) {
                    weapons.add(weapon);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public List<Weapon> getWeapons() {
        return weapons;
    }
}
