package org.dungeon.prototype.model.inventory;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.inventory.items.Weapon;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dungeon.prototype.model.inventory.attributes.weapon.Handling.SINGLE_HANDED;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Handling.TWO_HANDED;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Size.LARGE;

@Data
@NoArgsConstructor
public class WeaponSet {
    private String id;
    private Weapon primaryWeapon;
    private Weapon secondaryWeapon;
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

    public List<Weapon> getWeapons() {
        return Stream.of(primaryWeapon, secondaryWeapon)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
