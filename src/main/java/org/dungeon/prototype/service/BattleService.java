package org.dungeon.prototype.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.TurnUpdate;
import org.dungeon.prototype.model.effect.ExpirableEffect;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.properties.BattleProperties;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.MonsterService;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.dungeon.prototype.model.effect.attributes.EffectApplicant.MONSTER;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.MOVING;
import static org.dungeon.prototype.properties.CallbackType.ATTACK;
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
    private MessageService messageService;
    @Autowired
    private BattleProperties battleProperties;

    /**
     * Processes "attack" action, which performs attacking monster with selected weapon,
     * and his death or attack in response, which also may end up with death (of a player)
     *
     * @param chatId     id of current chat
     * @param attackType player's attack type, {@link CallbackType#ATTACK} or {@link CallbackType#SECONDARY_ATTACK}
     * @return true if processed with no exceptions
     */
    @TurnUpdate
    public boolean attack(Long chatId, CallbackType attackType) {
        var player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (!getMonsterRoomTypes().contains(currentRoom.getRoomContent().getRoomType())) {
            log.error("No monster to attack!");
            return false;
        }
        var monster = ((MonsterRoom) currentRoom.getRoomContent()).getMonster();
        log.debug("Attacking monster: {}", monster);
        playerAttacks(monster, player, attackType);

        if (monster.getHp() < 1) {
            log.debug("Monster killed!");
            levelService.updateAfterMonsterKill(currentRoom);
            player.addXp(monster.getXpReward());
        } else {
            monsterAttacks(player, monster);
            monsterService.saveOrUpdateMonster(monster);
        }
        playerService.updatePlayer(player);
        return messageService.sendRoomMessage(chatId, player, currentRoom);
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
        val inventory = player.getInventory();
        val chanceToDodge = player.getChanceToDodge();
        log.debug("Chance to dodge: {}", chanceToDodge);
        if (RandomUtil.flipAdjustedCoin(chanceToDodge)) {
            log.debug("Monster attack dodged!");
            return;
        }
        val attackTypeMap = battleProperties.getPlayerDefenseRatioMatrix().get(monsterAttack.getAttackType()).getMaterialDefenseRatioMap();
        log.debug("Monster attack: {}", monsterAttack.getAttackType());
        val diff = switch (monsterAttack.getAttackType()) {
            case SLASH, GROWL ->
                    (int) (monsterAttack.getAttack() * (inventory.getHelmet() == null ? 1.0 : attackTypeMap.get(inventory.getHelmet().getAttributes().getWearableMaterial())));
            default ->
                    (int) (monsterAttack.getAttack() * (inventory.getVest() == null ? 1.0 : attackTypeMap.get(inventory.getVest().getAttributes().getWearableMaterial())));
        };

        if (player.getDefense() > 0) {
            player.decreaseDefence(diff);
            log.debug("Player's armor decreased by: {}", 1);
        } else {
            player.decreaseHp(diff);
            log.debug("Player's health decreased by: {}", diff);
        }
    }

    private void playerAttacks(Monster monster, Player player, CallbackType attackType) {
        val attack = ATTACK.equals(attackType) ? player.getPrimaryAttack() :
                player.getSecondaryAttack();
        val chanceToMiss = attack.getChanceToMiss();
        log.debug("Chance to miss: {}", chanceToMiss);
        if (RandomUtil.flipAdjustedCoin(chanceToMiss)) {
            log.debug("Player missed!");
            return;
        }
        var attackPower = attack.getAttack();
        val criticalHitChance = attack.getCriticalHitChance();
        log.debug("Critical hit chance: {}", criticalHitChance);

        if (RandomUtil.flipAdjustedCoin(criticalHitChance)) {
            log.debug("Critical hit by player");
            attackPower = (int) (attackPower * attack.getCriticalHitMultiplier());
        }
        val knockOutChance = attack.getChanceToKnockOut();
        log.debug("Knock out chance: {}", knockOutChance);
        if (RandomUtil.flipAdjustedCoin(knockOutChance)) {
            val knockOut = new ExpirableEffect();
            knockOut.setApplicableTo(MONSTER);
            knockOut.setAttribute(MOVING);
            knockOut.setTurnsLasts(3);//TODO: configure
            monster.addEffect(knockOut);
        }
        log.debug("Player attacks with attack type: {}", attack.getAttackType());
        val monsterDefenseRatioMap = battleProperties.getMonsterDefenseRatioMatrix().get(attack.getAttackType()).getMonsterDefenseRatioMap();
        val decreaseAmount = (int) (attackPower * monsterDefenseRatioMap.get(monster.getMonsterClass()));
        monster.decreaseHp(decreaseAmount);
        log.debug("Monster health decreased by {}", decreaseAmount);
    }
}
