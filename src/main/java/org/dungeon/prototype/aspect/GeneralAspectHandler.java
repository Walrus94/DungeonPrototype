package org.dungeon.prototype.aspect;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.MonsterService;
import org.dungeon.prototype.service.room.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;


@Slf4j
@Aspect
@Component
public class GeneralAspectHandler {
    @Autowired
    private EffectService effectService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private MonsterService monsterService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ItemService itemService;

    @Before(value = "@annotation(org.dungeon.prototype.annotations.aspect.TurnUpdate)")
    public void beforeTurn(JoinPoint joinPoint) {
        handleBeforeTurn(joinPoint);
    }

    @Before(value = "@annotation(org.dungeon.prototype.annotations.aspect.RoomInitialization)")
    public void roomInitialization(JoinPoint joinPoint) {
        handleRoomInitialization(joinPoint);
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.BattleTurnUpdate)", returning = "result")
    public void afterBattleTurn(JoinPoint joinPoint, boolean result) {
        if (result) {
            handleAfterBattleTurn(joinPoint);
        }
    }

    private void handleRoomInitialization(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long) {
            if (args.length > 1 && args[1] instanceof Player) {
                if (args.length > 2 && args[2] instanceof Room currentRoom) {
                    if (currentRoom.getRoomContent() instanceof  MonsterRoom monsterRoom) {
                        val monster = monsterRoom.getMonster();
                        if (isNull(monster.getAttackPattern()) || monster.getAttackPattern().isEmpty()) {
                            monster.setAttackPattern(monster.getDefaultAttackPattern());
                        }
                        monster.setCurrentAttack(monster.getAttackPattern().poll());
                        monsterService.saveOrUpdateMonster(monster);
                        roomService.saveOrUpdateRoomContent(monsterRoom);
                    }
                }
            }
        }
    }

    private void handleBeforeTurn(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long) {
            if (args.length > 1 && args[1] instanceof Player player) {
                playerService.updatePlayer(effectService.updatePlayerEffects(player));
            }
        }
    }

    private void handleAfterBattleTurn(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            if (args.length > 1 && args[1] instanceof Player player) {
                if (player.getHp() < 1) {
                    levelService.remove(chatId);
                    player.getInventory().clear();
                    itemService.dropCollection(chatId);
                    player.getEffects().clear();
                    playerService.updatePlayer(player);
                    messageService.sendDeathMessage(chatId);
                    return;
                }
                if (args.length > 2 && args[2] instanceof Room room) {
                    if (room.getRoomContent() instanceof MonsterRoom monsterRoom) {
                        val monster = monsterRoom.getMonster();
                        if (monster.getHp() < 1) {
                            levelService.updateAfterMonsterKill(room);
                            val newLevelAchieved = player.addXp(monster.getXpReward());
                            if (newLevelAchieved) {
                                messageService.sendLevelUpgradeMessage(chatId, player);
                            }
                        } else {
                            if (isNull(monster.getAttackPattern()) || monster.getAttackPattern().isEmpty()) {
                                monster.setAttackPattern(monster.getDefaultAttackPattern());
                            }
                            monster.setCurrentAttack(monster.getAttackPattern().poll());
                            monsterService.saveOrUpdateMonster(effectService.updateMonsterEffects(monsterRoom.getMonster()));
                        }
                    }
                }
            }
        }
    }
}
