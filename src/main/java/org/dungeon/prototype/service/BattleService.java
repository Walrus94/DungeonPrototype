package org.dungeon.prototype.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.effect.MonsterEffect;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.properties.BattleProperties;
import org.dungeon.prototype.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Objects.isNull;
import static org.dungeon.prototype.model.effect.attributes.MonsterEffectAttribute.MOVING;
import static org.dungeon.prototype.model.player.PlayerAttribute.POWER;

@Slf4j
@Service
public class BattleService {

    @Autowired
    private BattleProperties battleProperties;

    /**
     *
     * @param player player being attacked
     * @param monster that attacks
     * @return player after monster attack
     */
    public Player monsterAttacks(Player player, Monster monster) {
        if (monster.getEffects().stream().anyMatch(monsterEffect -> MOVING.equals(monsterEffect.getAttribute()))) {
            return player;
        }
        if (isNull(monster.getAttackPattern()) || !monster.getAttackPattern().hasNext()) {
            monster.setAttackPattern(monster.getDefaultAttackPattern());
        }
        monster.setCurrentAttack(monster.getAttackPattern().next());

        val monsterAttack = monster.getCurrentAttack();
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
            case SLASH, GROWL ->
                    (int) (monsterAttack.getAttack() * (armorSet.getHelmet() == null ? 1.0 : attackTypeMap.get(armorSet.getHelmet().getAttributes().getWearableMaterial())));
            default ->
                    (int) (monsterAttack.getAttack() * (armorSet.getVest() == null ? 1.0 : attackTypeMap.get(armorSet.getVest().getAttributes().getWearableMaterial())));
        };

        if (player.getDefense() > 0) {
            player.decreaseDefence(1);
            log.debug("Player's armor decreased by: {}", 1);
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
        Integer playerAttack;
        if (isNull(weapon)) {
            playerAttack = player.getAttributes().get(POWER);
            log.debug("Player attacks with bare hands!");
            monster.decreaseHp(playerAttack);
            log.debug("Monster health decreased by {}", playerAttack);
            return monster;
        } else {
            playerAttack = player.getAttributes().get(POWER) + weapon.getAttack();
            val chanceToMiss = weapon.getChanceToMiss();
            log.debug("Chance to miss: {}", chanceToMiss);
            if (RandomUtil.flipAdjustedCoin(chanceToMiss)) {
                log.debug("Player missed!");
                return monster;
            }
            val criticalHitChance = weapon.getCriticalHitChance();
            log.debug("Critical hit chance: {}", criticalHitChance);

            if (RandomUtil.flipAdjustedCoin(criticalHitChance)) {
                log.debug("Critical hit by player");
                playerAttack *= 2;//TODO: configure critical hit
            }
            val knockOutChance = weapon.getChanceToKnockOut();
            log.debug("Knock out chance: {}", knockOutChance);
            if (RandomUtil.flipAdjustedCoin(knockOutChance)) {
                val knockOut = new MonsterEffect();
                knockOut.setAttribute(MOVING);
                knockOut.setTurnsLasts(3);//TODO: configure
                monster.addEffect(knockOut);
            }
            log.debug("Player attacks with {}, attack type: {}", weapon.getName(), weapon.getAttributes().getWeaponAttackType());
            val monsterDefenseRatioMap = battleProperties.getMonsterDefenseRatioMatrix().get(weapon.getAttributes().getWeaponAttackType()).getMonsterDefenseRatioMap();
            val decreaseAmount = (int) (playerAttack * monsterDefenseRatioMap.get(monster.getMonsterClass()));
            monster.decreaseHp(decreaseAmount);
            log.debug("Monster health decreased by {}", decreaseAmount);
            return monster;
        }
    }
}
