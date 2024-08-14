package org.dungeon.prototype.aspect;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.service.MonsterService;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.level.LevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
public class StartTurnAspect {
    @Autowired
    private EffectService effectService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private MonsterService monsterService;
    @Autowired
    private PlayerService playerService;

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.TurnUpdate)", returning = "result")
    public void afterTurn(JoinPoint joinPoint, Boolean result) {
        if (result) {
            handleTurnUpdate(joinPoint);
        }
    }

    @Before(value = "@annotation(org.dungeon.prototype.annotations.aspect.TurnMonsterRoomUpdate)")
    public void beforeBattleTurn(JoinPoint joinPoint) {
        handleMonsterRoomTurnUpdate(joinPoint);
    }

    private void handleTurnUpdate(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            if (args.length > 1 && args[1] instanceof Player player) {
                log.debug("Updating expirable effects for player in chat : {}", chatId);
                playerService.updatePlayer(effectService.updatePlayerEffects(player));
            }
        }
    }

    private void handleMonsterRoomTurnUpdate(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        if (args.length > 0 && args[0] instanceof Long chatId) {
            if (args.length > 1 && args[1] instanceof Player player) {
                effectService.updatePlayerEffects(player);
                if (args.length > 2 && args[2] instanceof Level level) {
                    if (args.length > 3 && args[3] instanceof Room nextRoom) {
                        val monster = (Monster) nextRoom.getRoomContent();
                        level.setActiveMonster(monster);
                        monsterService.saveOrUpdateMonster(effectService.updateMonsterEffects(monster));
                        levelService.saveOrUpdateLevel(level);

                    }
                }
                log.debug("Updating expirable effects for player and monster in chat : {}", chatId);
            }
        }
    }
}
