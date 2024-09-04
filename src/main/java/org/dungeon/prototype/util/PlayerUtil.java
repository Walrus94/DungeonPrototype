package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttack;

import static java.util.Objects.nonNull;
import static org.dungeon.prototype.model.player.PlayerAttribute.POWER;

@UtilityClass
public class PlayerUtil {
    public static PlayerAttack getSecondaryAttack(Player player, Weapon secondaryWeapon) {
        if (nonNull(secondaryWeapon)) {
            PlayerAttack playerAttack = new PlayerAttack();
            setAttackValues(player, secondaryWeapon, playerAttack);
            return playerAttack;
        }
        return null;
    }

    public static PlayerAttack getPrimaryAttack(Player player, Weapon primaryWeapon) {
        PlayerAttack playerAttack = new PlayerAttack();
        if (nonNull(primaryWeapon)) {
            setAttackValues(player, primaryWeapon, playerAttack);
        } else {
            playerAttack.setAttack(player.getAttributes().get(POWER));
        }
        return playerAttack;
    }

    private static void setAttackValues(Player player, Weapon weapon, PlayerAttack playerAttack) {
        playerAttack.setAttack(weapon.getAttack() + player.getAttributes().get(POWER));
        playerAttack.setAttackType(weapon.getAttributes().getWeaponAttackType());
        playerAttack.setCriticalHitChance(weapon.getCriticalHitChance());
        playerAttack.setCriticalHitMultiplier(weapon.getCriticalHitMultiplier());
        playerAttack.setChanceToMiss(weapon.getChanceToMiss());
        playerAttack.setChanceToKnockOut(weapon.getChanceToKnockOut());
    }
}
