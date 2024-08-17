package org.dungeon.prototype.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.SendRoomMessage;
import org.dungeon.prototype.annotations.aspect.AfterTurnUpdate;
import org.dungeon.prototype.model.effect.MonsterEffect;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.properties.BattleProperties;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.dungeon.prototype.model.effect.attributes.MonsterEffectAttribute.MOVING;
import static org.dungeon.prototype.model.player.PlayerAttribute.POWER;
import static org.dungeon.prototype.util.RoomGenerationUtils.getMonsterRoomTypes;

@Slf4j
@Service
public class BattleService {

    @Autowired
    private PlayerService playerService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private MonsterService monsterService;
    @Autowired
    private BattleProperties battleProperties;

    @AfterTurnUpdate
    @SendRoomMessage
    public boolean attack(Long chatId, boolean isPrimaryAttack) {
        var player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (!getMonsterRoomTypes().contains(currentRoom.getRoomContent().getRoomType())) {
            log.error("No monster to attack!");
            return false;
        }
        var monster = ((MonsterRoom) currentRoom.getRoomContent()).getMonster();
        log.debug("Attacking monster: {}", monster);
        playerAttacks(monster, player, isPrimaryAttack);

        if (monster.getHp() < 1) {
            log.debug("Monster killed!");
            levelService.updateAfterMonsterKill(level, currentRoom);
            player.addXp(monster.getXpReward());
        } else {
            monsterAttacks(player, monster);
            monsterService.saveOrUpdateMonster(monster);
        }
        playerService.updatePlayer(player);
        return true;
    }

    private void monsterAttacks(Player player, Monster monster) {
        if (nonNull(monster.getEffects()) && monster.getEffects().stream().anyMatch(monsterEffect -> MOVING.equals(monsterEffect.getAttribute()))) {
            return;
        }
        if (isNull(monster.getAttackPattern()) || monster.getAttackPattern().isEmpty()) {
            monster.setAttackPattern(monster.getDefaultAttackPattern());
        }
        monster.setCurrentAttack(monster.getAttackPattern().poll());

        val monsterAttack = monster.getCurrentAttack();
        val armorSet = player.getInventory().getArmorSet();
        val chanceToDodge = armorSet.getBoots() == null ? 0.0 : armorSet.getBoots().getChanceToDodge();
        log.debug("Chance to dodge: {}", chanceToDodge);
        if (RandomUtil.flipAdjustedCoin(chanceToDodge)) {
            log.debug("Monster attack dodged!");
            return;
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
    }

    private void playerAttacks(Monster monster, Player player, boolean isPrimaryAttack) {
        val inventory = player.getInventory();
        val weapon = isPrimaryAttack ? inventory.getWeaponSet().getPrimaryWeapon() :
                inventory.getWeaponSet().getSecondaryWeapon();
        Integer playerAttack;
        if (isNull(weapon)) {
            playerAttack = player.getAttributes().get(POWER);
            log.debug("Player attacks with bare hands!");
            monster.decreaseHp(playerAttack);
            log.debug("Monster health decreased by {}", playerAttack);
        } else {
            playerAttack = player.getAttributes().get(POWER) + weapon.getAttack();
            val chanceToMiss = weapon.getChanceToMiss();
            log.debug("Chance to miss: {}", chanceToMiss);
            if (RandomUtil.flipAdjustedCoin(chanceToMiss)) {
                log.debug("Player missed!");
                return;
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
        }
    }
}
