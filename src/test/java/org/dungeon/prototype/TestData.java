package org.dungeon.prototype;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.weapon.Handling;
import org.dungeon.prototype.model.inventory.attributes.weapon.Size;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponHandlerMaterial;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponMaterial;
import org.dungeon.prototype.model.inventory.items.Usable;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.monster.MonsterAttack;
import org.dungeon.prototype.model.monster.MonsterAttackType;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.ui.level.LevelMap;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.dungeon.prototype.model.Direction.E;
import static org.dungeon.prototype.model.Direction.N;
import static org.dungeon.prototype.model.Direction.S;
import static org.dungeon.prototype.model.Direction.W;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.SLASH;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponType.SWORD;
import static org.dungeon.prototype.model.player.PlayerAttribute.LUCK;
import static org.dungeon.prototype.model.player.PlayerAttribute.MAGIC;
import static org.dungeon.prototype.model.player.PlayerAttribute.PERCEPTION;
import static org.dungeon.prototype.model.player.PlayerAttribute.POWER;
import static org.dungeon.prototype.model.player.PlayerAttribute.STAMINA;

@UtilityClass
public class TestData {

    public static Level getLevel() {
        val level = new Level();
        val start = new Room();
        start.setPoint(new Point(5, 6));
        start.setAdjacentRooms(new EnumMap<>(Map.of(
                N, true,
                E, false,
                S, false,
                W, false)));
        level.setStart(start);
        val levelMap = new LevelMap();
        level.setLevelMap(levelMap);
        return level;
    }

    public static Monster getMonster(Integer health, Integer maxHealth) {
        val monster = new Monster();
        monster.setMonsterClass(MonsterClass.ZOMBIE);
        monster.setPrimaryAttack(MonsterAttack.of(MonsterAttackType.SLASH, 5));
        monster.setSecondaryAttack(MonsterAttack.of(MonsterAttackType.SLASH, 5));
        monster.setAttackPattern(monster.getDefaultAttackPattern());
        monster.setHp(health);
        monster.setMaxHp(maxHealth);
        monster.setXpReward(300);
        return monster;
    }

    public static Player getPlayer(Long chatId) {
        return getPlayer(chatId, "default_room_id", 50, 50);
    }

    public static Player getPlayer(Long chatId, String currentRoomId) {
        return getPlayer(chatId, currentRoomId, 50, 50);
    }
    public static Player getPlayer(Long chatId, String currentRoomId, Integer health, Integer maxHealth) {
        val player = new Player();
        player.setId("player_id");
        player.setChatId(chatId);
        player.setDirection(N);
        player.setGold(100);
        player.setEffects(new ArrayList<>());
        player.setCurrentRoomId(currentRoomId);
        player.setCurrentRoom(new Point(5, 6));
        player.setAttributes(new EnumMap<>(Map.of(
                POWER, 5,
                STAMINA, 4,
                PERCEPTION, 3,
                MAGIC, 1,
                LUCK, 2
        )));
        player.setInventory(getInventory());
        player.setDefense(0);
        player.setMaxDefense(10);
        player.setXp(100L);
        player.setPlayerLevel(1);
        player.setNextLevelXp(1200L);
        player.setHp(health);
        player.setMaxHp(maxHealth);
        player.setMana(10);
        player.setMaxMana(10);
        return player;
    }

    public static Inventory getInventory() {
        val inventory = new Inventory();
        inventory.setPrimaryWeapon(getWeapon());
        return inventory;
    }

    public static Weapon getWeapon() {
        val weapon = new Weapon();
        weapon.setId("item_id");
        val weaponAttributes = new WeaponAttributes();
        weaponAttributes.setWeaponType(SWORD);
        weaponAttributes.setWeaponAttackType(SLASH);
        weaponAttributes.setSize(Size.MEDIUM);
        weaponAttributes.setHandling(Handling.SINGLE_HANDED);
        weaponAttributes.setWeaponMaterial(WeaponMaterial.IRON);
        weaponAttributes.setWeaponHandlerMaterial(WeaponHandlerMaterial.LEATHER);
        weapon.setAttack(5);
        weapon.setChanceToMiss(0.0);
        weapon.setCriticalHitChance(0.0);
        weapon.setAttributes(weaponAttributes);
        weapon.setEffects(new ArrayList<>());
        return weapon;
    }

    public static Merchant getMerchant() {
        Merchant merchant = new Merchant();
        Set<Item> items = getItems();
        merchant.setItems(items);
        return merchant;
    }

    public static Set<Item> getItems() {
        Set<Item> items = new HashSet<>();
        val item1 = new Weapon();
        item1.setId("itemId1");
        item1.setEffects(new ArrayList<>());
        items.add(item1);
        val item2 = new Wearable();
        item2.setId("itemId2");
        item2.setEffects(new ArrayList<>());
        items.add(item2);
        val item3 = new Usable();
        item3.setId("itemId3");
        item3.setEffects(new ArrayList<>());
        items.add(item3);

        return items;
    }
}
