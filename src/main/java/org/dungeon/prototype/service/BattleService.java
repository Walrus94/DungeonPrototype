package org.dungeon.prototype.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.monster.MonsterAttack;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.properties.BattleProperties;
import org.dungeon.prototype.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BattleService {

    @Autowired
    BattleProperties battleProperties;

    public Player monsterAttacks(Player player, MonsterAttack monsterAttack) {
        val armorSet = player.getInventory().getArmorSet();
        val chanceToDodge = armorSet.getBoots() == null ? 0.0 : armorSet.getBoots().getChanceToDodge();
        log.debug("Chance to dodge: {}", chanceToDodge);
        if (RandomUtil.flipAdjustedCoin(chanceToDodge)) {
            log.debug("Monster attack dodged!");
            return player;
        }
        val attackTypeMap = battleProperties.getPlayerDefenseRatioMatrix().get(monsterAttack.getAttackType()).getMaterialDefenseRatioMap();
        log.debug("Monster attack: {}", monsterAttack.getAttackType());
        val diff = switch (monsterAttack.getAttackType()) {
            case SLASH, GROWL -> (int) (monsterAttack.getAttack() * (armorSet.getHelmet() == null ? 1.0 : attackTypeMap.get(armorSet.getHelmet().getAttributes().getWearableMaterial())));
            default -> (int) (monsterAttack.getAttack() * (armorSet.getVest() == null ? 1.0 : attackTypeMap.get(armorSet.getVest().getAttributes().getWearableMaterial())));
        };

        if (player.getDefense() > 0) {
            player.decreaseDefence(diff);
            log.debug("Player's armor decreased by: {}", diff);
        } else {
            player.decreaseHp(diff);
            log.debug("Player's health decreased by: {}", diff);
        }
        return player;
    }

    public Monster playerAttacks(Monster monster, Player player, boolean isPrimaryAttack) {
        val inventory = player.getInventory();
        val weapon = isPrimaryAttack ? inventory.getWeaponSet().getPrimaryWeapon() :
                inventory.getWeaponSet().getSecondaryWeapon();

        log.debug("Player attacks with {}, attack type: {}", weapon.getName(), weapon.getAttributes().getWeaponAttackType());

        val monsterDefenseRatioMap = battleProperties.getMonsterDefenseRatioMatrix().get(weapon.getAttributes().getWeaponAttackType()).getMonsterDefenseRatioMap();
        val decreaseAmount = (int) (weapon.getAttack() * monsterDefenseRatioMap.get(monster.getMonsterClass()));
        monster.decreaseHp(decreaseAmount);
        log.debug("Monster health decreased by {}", decreaseAmount);
        return monster;
    }

    public void firstHitAttacked(Monster monster, Integer additionalFirstHit, WeaponAttackType weaponAttackType) {
        log.debug("Player strikes additional hit!");
        val monsterDefenseRatioMap = battleProperties.getMonsterDefenseRatioMatrix().get(weaponAttackType).getMonsterDefenseRatioMap();
        val decreaseAmount = (int) (additionalFirstHit * monsterDefenseRatioMap.get(monster.getMonsterClass()));
        monster.decreaseHp(decreaseAmount);
        log.debug("Monster health decreased by {}", decreaseAmount);
    }
}
