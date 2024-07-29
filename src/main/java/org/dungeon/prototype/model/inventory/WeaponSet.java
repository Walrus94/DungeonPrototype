package org.dungeon.prototype.model.inventory;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.inventory.items.Weapon;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dungeon.prototype.model.inventory.attributes.weapon.Handling.SINGLE_HANDED;

@Data
@NoArgsConstructor
public class WeaponSet {
    private String id;
    private Weapon primaryWeapon;
    private Weapon secondaryWeapon;

    public boolean addWeapon(Weapon weapon) {
        switch (weapon.getAttributes().getHandling()) {
            case SINGLE_HANDED, TWO_HANDED -> {
                if (Objects.nonNull(primaryWeapon)) {
                    return false;
                }
                primaryWeapon = weapon;
                return true;
            }
            case ADDITIONAL -> {
                if (Objects.nonNull(primaryWeapon) && SINGLE_HANDED.equals(primaryWeapon.getAttributes().getHandling())) {
                    secondaryWeapon = weapon;
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public List<Weapon> getWeapons() {
        return Stream.of(primaryWeapon, secondaryWeapon).collect(Collectors.toList());
    }
}
