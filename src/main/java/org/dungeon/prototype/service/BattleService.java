package org.dungeon.prototype.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.BattleTurnUpdate;
import org.dungeon.prototype.annotations.aspect.TurnUpdate;
import org.dungeon.prototype.model.effect.ExpirableAdditionEffect;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.service.balancing.BalanceMatrixService;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.MonsterService;
import org.dungeon.prototype.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.MOVING;
import static org.dungeon.prototype.properties.CallbackType.ATTACK;

@Slf4j
@Service
public class BattleService {

    @Autowired
    private PlayerService playerService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private MonsterService monsterService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private BalanceMatrixService balanceMatrixService;

    /**
     * Processes "attack" action, which performs attacking monster with selected weapon,
     * and his death or attack in response, which also may end up with death (of a player)
     *
     * @param chatId    id of current chat
     * @param player    current player
     * @param currentRoom monster room
     * @param attackType player's attack type, {@link CallbackType#ATTACK} or {@link CallbackType#SECONDARY_ATTACK}
     */
    @TurnUpdate
    @BattleTurnUpdate
    public void attack(Long chatId, Player player, Room currentRoom, CallbackType attackType) {
        var monster = ((MonsterRoom) currentRoom.getRoomContent()).getMonster();
        log.info("Attacking monster: {}", monster);
        playerAttacks(monster, player, attackType);

        if (monster.getHp() < 1) {
            log.info("Monster killed!");
            levelService.updateAfterMonsterKill(currentRoom);
            val newLevelAchieved = player.addXp(monster.getXpReward());
            if (newLevelAchieved) {
                messageService.sendLevelUpgradeMessage(chatId, player);
            } else {
                playerService.updatePlayer(player);
                messageService.sendRoomMessage(chatId, player, currentRoom);
            }
        } else {
            monsterAttacks(player, monster);
            playerService.updatePlayer(player);
            messageService.sendMonsterRoomMessage(chatId, player, currentRoom);
        }
    }

    private void monsterAttacks(Player player, Monster monster) {
        val attackMatrix = balanceMatrixService.getMonsterAttackMatrix(player.getChatId());
        val defenseMatrix = balanceMatrixService.getPlayerDefenceMatrix(player.getChatId());
        if (nonNull(monster.getEffects()) && monster.getEffects().stream().anyMatch(monsterEffect -> MOVING.equals(monsterEffect.getAttribute()))) {
            return;
        }
        if (isNull(monster.getAttackPattern()) || monster.getAttackPattern().isEmpty()) {
            monster.setAttackPattern(monster.getDefaultAttackPattern());
        }
        monster.setCurrentAttack(monster.getAttackPattern().poll());

        val monsterAttack = monster.getCurrentAttack();
        val inventory = player.getInventory();
        val chanceToDodge = player.getChanceToDodge();
        log.info("Chance to dodge: {}", chanceToDodge);
        if (RandomUtil.flipAdjustedCoin(chanceToDodge)) {
            log.info("Monster attack dodged!");
            return;
        }
        val defense = defenseMatrix[monsterAttack.getAttackType().ordinal()][player.getInventory().getVest().getAttributes().getWearableMaterial().ordinal()];
        log.info("Monster attack: {}", monsterAttack.getAttackType());
        val diff = switch (monsterAttack.getAttackType()) {
            case SLASH, GROWL ->
                    (int) (monsterAttack.getAttack() * (inventory.getHelmet() == null ? 1.0 : attackMatrix[inventory.getHelmet().getAttributes().getWearableMaterial().ordinal()][monsterAttack.getAttackType().ordinal()]) / defense);
            default ->
                    (int) (monsterAttack.getAttack() * (inventory.getVest() == null ? 1.0 : attackMatrix[inventory.getVest().getAttributes().getWearableMaterial().ordinal()][monsterAttack.getAttackType().ordinal()])/ defense);
        };

        if (player.getDefense() > 0) {
            player.decreaseDefence(diff);
            log.info("Player's armor decreased by: {}", 1);
        } else {
            player.decreaseHp(diff);
            log.info("Player's health decreased by: {}", diff);
        }
        monsterService.updateMonster(monster);
    }

    private void playerAttacks(Monster monster, Player player, CallbackType attackType) {
        val attackMatrix = balanceMatrixService.getPlayerAttackMatrix(player.getChatId());
        val defenseMatrix = balanceMatrixService.getMonsterDefenseMatrix(player.getChatId());
        val attack = ATTACK.equals(attackType) ? player.getPrimaryAttack() :
                player.getSecondaryAttack();
        val chanceToMiss = attack.getChanceToMiss();
        log.info("Chance to miss: {}", chanceToMiss);
        if (RandomUtil.flipAdjustedCoin(chanceToMiss)) {
            log.info("Player missed!");
            return;
        }
        var attackPower = attack.getAttack() * attackMatrix[attack.getAttackType().ordinal()][monster.getMonsterClass().ordinal()];
        val criticalHitChance = attack.getCriticalHitChance();
        log.info("Critical hit chance: {}", criticalHitChance);

        if (RandomUtil.flipAdjustedCoin(criticalHitChance)) {
            log.info("Critical hit by player");
            attackPower = (int) (attackPower * attack.getCriticalHitMultiplier());
        }
        val knockOutChance = attack.getChanceToKnockOut();
        log.info("Knock out chance: {}", knockOutChance);
        if (RandomUtil.flipAdjustedCoin(knockOutChance)) {
            val knockOut = ExpirableAdditionEffect.builder()
                    .attribute(MOVING)
                    .turnsLeft(3)
                    .build();
            monster.addEffect(knockOut);
        }
        log.info("Player attacks with attack type: {}", attack.getAttackType());
        val monsterDefense = defenseMatrix[attack.getAttackType().ordinal()][monster.getMonsterClass().ordinal()];
        val decreaseAmount = (int) (attackPower * monsterDefense);
        monster.decreaseHp(decreaseAmount);
        log.info("Monster health decreased by {}", decreaseAmount);
    }
}
