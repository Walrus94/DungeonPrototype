package org.dungeon.prototype;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.inventory.ArmorSet;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.WeaponSet;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.monster.MonsterAttack;
import org.dungeon.prototype.model.monster.MonsterAttackType;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.player.Player;

import java.util.EnumMap;
import java.util.Map;

import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.SLASH;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponType.SWORD;
import static org.dungeon.prototype.model.player.PlayerAttribute.LUCK;
import static org.dungeon.prototype.model.player.PlayerAttribute.MAGIC;
import static org.dungeon.prototype.model.player.PlayerAttribute.PERCEPTION;
import static org.dungeon.prototype.model.player.PlayerAttribute.POWER;
import static org.dungeon.prototype.model.player.PlayerAttribute.STAMINA;

@UtilityClass
public class TestUtils {

    public static Level getLevel() {
        return new Level();
    }

    public static Monster getMonster() {
        val monster = new Monster();
        monster.setMonsterClass(MonsterClass.ZOMBIE);
        monster.setPrimaryAttack(MonsterAttack.of(MonsterAttackType.SLASH, 5));
        monster.setSecondaryAttack(MonsterAttack.of(MonsterAttackType.SLASH, 5));
        monster.setAttackPattern(monster.getDefaultAttackPattern());
        monster.setHp(10);
        monster.setMaxHp(15);
        return monster;
    }
    public static Player getPlayer(Long chatId, String currentRoomId) {
        val player = new Player();
        player.setId("player_id");
        player.setChatId(chatId);
        player.setCurrentRoomId(currentRoomId);
        player.setAttributes(new EnumMap<>(Map.of(
                POWER, 5,
                STAMINA, 4,
                PERCEPTION, 3,
                MAGIC, 1,
                LUCK, 2
        )));
        player.setInventory(getInventory());
        player.setDefense(0);
        player.setHp(20);
        return player;
    }

    public static Inventory getInventory() {
        val inventory = new Inventory();
        val weaponSet = new WeaponSet();
        val armorSet = new ArmorSet();
        weaponSet.setPrimaryWeapon(getWeapon());
        inventory.setWeaponSet(weaponSet);
        inventory.setArmorSet(armorSet);
        return inventory;
    }

    public static Weapon getWeapon() {
        val weapon = new Weapon();
        val weaponAttributes = new WeaponAttributes();
        weaponAttributes.setWeaponType(SWORD);
        weaponAttributes.setWeaponAttackType(SLASH);
        weapon.setAttack(5);
        weapon.setChanceToMiss(0.0);
        weapon.setCriticalHitChance(0.0);
        weapon.setAttributes(weaponAttributes);
        return weapon;
    }
}
