package org.dungeon.prototype;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.dungeon.prototype.model.level.Level;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.ExpirableMultiplicationEffect;
import org.dungeon.prototype.model.effect.attributes.EffectApplicant;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.model.effect.ExpirableAdditionEffect;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.dungeon.prototype.model.inventory.attributes.Quality;
import org.dungeon.prototype.model.inventory.attributes.weapon.Handling;
import org.dungeon.prototype.model.inventory.attributes.weapon.Size;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponHandlerMaterial;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponMaterial;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.model.inventory.items.Usable;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.monster.MonsterAttack;
import org.dungeon.prototype.model.monster.MonsterAttackType;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttack;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.level.ui.LevelMap;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
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

    public static Level getLevel(int levelNumber) {
        val level = new Level();
        level.setNumber(levelNumber);
        val start = new Room();
        start.setId("startRoomId");
        start.setPoint(new Point(0, 0));
        start.setAdjacentRooms(new EnumMap<>(Map.of(
                N, true,
                E, false,
                S, false,
                W, false)));
        level.setStart(start.getPoint());
        val end = new Room();
        end.setId("endRoomId");
        end.setPoint(new Point(5, 5));
        end.setAdjacentRooms(new EnumMap<>(Map.of(
                N, true,
                E, false,
                S, false,
                W, false)));
        level.setEnd(end.getPoint());
        level.setRoomsMap(Map.of(start.getPoint(), start, end.getPoint(), end));
        val levelMap = new LevelMap();
        level.setLevelMap(levelMap);
        return level;
    }

    public static Monster getMonster(Integer health, Integer maxHealth) {
        val monster = Monster.builder()
                .monsterClass(MonsterClass.ZOMBIE)
                .primaryAttack(MonsterAttack.builder()
                        .attackType(MonsterAttackType.SLASH)
                        .effect(ExpirableAdditionEffect.builder()
                                .isAccumulated(true)
                                .amount(-5)
                                .attribute(EffectAttribute.HEALTH)
                                .build())
                        .attack(5)
                        .criticalHitChance(0.0)
                        .criticalHitMultiplier(0.0)
                        .chanceToMiss(0.0)
                        .chanceToKnockOut(0.0)
                        .causingEffectProbability(0.0)
                        .build())
                .secondaryAttack(MonsterAttack.builder()
                        .attackType(MonsterAttackType.SLASH)
                        .effect(ExpirableAdditionEffect.builder()
                                .amount(2)
                                .turnsLeft(3)
                                .isAccumulated(true)
                                .attribute(EffectAttribute.HEALTH)
                                .build())
                        .attack(5)
                        .criticalHitChance(0.0)
                        .criticalHitMultiplier(0.0)
                        .chanceToMiss(0.0)
                        .chanceToKnockOut(0.0)
                        .causingEffectProbability(0.0)
                        .build())
                .hp(health)
                .maxHp(maxHealth)
                .effects(new ArrayList<>())
                .build();
        monster.setAttackPattern(monster.getDefaultAttackPattern());
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
        PlayerAttack playerAttack = new PlayerAttack();
        playerAttack.setAttack(5);
        playerAttack.setAttackType(SLASH);
        playerAttack.setCriticalHitChance(0.0);
        playerAttack.setCriticalHitMultiplier(1.0);
        playerAttack.setChanceToMiss(0.0);
        playerAttack.setChanceToKnockOut(0.0);
        player.setPrimaryAttack(playerAttack);
        player.setDefense(0);
        player.setMaxDefense(10);
        player.setXp(100L);
        player.setPlayerLevel(1);
        player.setNextLevelXp(1200L);
        player.setHp(health);
        player.setMaxHp(maxHealth);
        player.setMana(5);
        player.setMaxMana(10);
        return player;
    }

    public static Inventory getInventory() {
        val inventory = new Inventory();
        inventory.setPrimaryWeapon(getWeapon());
        inventory.setVest(getVest());
        inventory.setBoots(getBoots());
        inventory.setItems(new ArrayList<>());
        return inventory;
    }

    private static Wearable getVest() {
        Wearable vest = new Wearable();
        WearableAttributes attributes = new WearableAttributes();
        attributes.setWearableType(WearableType.VEST);
        attributes.setQuality(Quality.COMMON);
        attributes.setWearableMaterial(WearableMaterial.CLOTH);
        vest.setAttributes(attributes);
        vest.setArmor(5);
        vest.setEffects(new ArrayList<>());
        vest.setMagicType(MagicType.of(0.0,0.0));
        return vest;
    }

    private static Wearable getBoots() {
        Wearable boots = new Wearable();
        WearableAttributes attributes = new WearableAttributes();
        attributes.setWearableType(WearableType.BOOTS);
        attributes.setQuality(Quality.COMMON);
        attributes.setWearableMaterial(WearableMaterial.LEATHER);
        boots.setAttributes(attributes);
        boots.setChanceToDodge(0.1);
        boots.setArmor(0);
        boots.setEffects(new ArrayList<>());
        boots.setMagicType(MagicType.of(0.0,0.0));
        return boots;
    }

    public static Weapon getWeapon() {
        val weapon = new Weapon();
        weapon.setId("weapon_id");
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
        weapon.setCriticalHitMultiplier(1.0);
        weapon.setChanceToKnockOut(0.0);
        weapon.setAttributes(weaponAttributes);
        weapon.setEffects(new ArrayList<>());
        weapon.setMagicType(MagicType.of(0.0,0.0));
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

    public static List<ExpirableAdditionEffect> getMonsterEffects() {
        List<ExpirableAdditionEffect> effects = new ArrayList<>();
        ExpirableAdditionEffect knockOut = ExpirableAdditionEffect.builder()
                .turnsLeft(3)
                .isAccumulated(true)
                .applicableTo(EffectApplicant.MONSTER)
                .attribute(EffectAttribute.MOVING)
                .build();
        effects.add(knockOut);

        ExpirableAdditionEffect regeneration = ExpirableAdditionEffect.builder()
                .turnsLeft(2)
                .amount(5)
                .isAccumulated(true)
                .attribute(EffectAttribute.HEALTH)
                .applicableTo(EffectApplicant.MONSTER)
                .build();
        effects.add(regeneration);
        return effects;
    }

    public static List<Effect> getPlayerEffects() {
        List<Effect> effects = new ArrayList<>();
        ExpirableAdditionEffect manaRegeneration = ExpirableAdditionEffect.builder()
                .attribute(EffectAttribute.MANA)
                .turnsLeft(3)
                .amount(1)
                .isAccumulated(true)
                .applicableTo(EffectApplicant.PLAYER)
                .build();
        effects.add(manaRegeneration);

        ExpirableMultiplicationEffect weaponEffect = ExpirableMultiplicationEffect.builder()
                .turnsLeft(1)
                .attribute(EffectAttribute.CRITICAL_HIT_CHANCE)
                .multiplier(1.07)
                .applicableTo(EffectApplicant.PLAYER)
                .build();
        effects.add(weaponEffect);

        ExpirableMultiplicationEffect wearableEffect = ExpirableMultiplicationEffect.builder()
                .turnsLeft(2)
                .applicableTo(EffectApplicant.PLAYER)
                .attribute(EffectAttribute.CHANCE_TO_DODGE)
                .multiplier(1.05)
                .build();
        effects.add(wearableEffect);

        ExpirableAdditionEffect armorEffect = ExpirableAdditionEffect.builder()
                .applicableTo(EffectApplicant.PLAYER)
                .attribute(EffectAttribute.MAX_ARMOR)
                .amount(5)
                .turnsLeft(3)
                .isAccumulated(false)
                .build();
        effects.add(armorEffect);

        return effects;
    }
}
