package org.dungeon.prototype.model.inventory.attributes.weapon;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.item.ItemAttributes;
import org.dungeon.prototype.model.inventory.attributes.Quality;

@Data
@NoArgsConstructor
public class WeaponAttributes implements ItemAttributes {
    private WeaponType weaponType;
    private Handling handling;
    private WeaponMaterial weaponMaterial;
    private WeaponHandlerMaterial weaponHandlerMaterial;
    private Quality quality;
    private Size size;
    private WeaponAttackType weaponAttackType;
}