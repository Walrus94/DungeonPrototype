package org.dungeon.prototype.aspect;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.service.PlayerLevelService;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.MonsterService;
import org.dungeon.prototype.service.room.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
public class TurnUpdateAspectHandler {
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

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.TurnUpdate)", returning = "result")
    public void afterTurn(JoinPoint joinPoint, boolean result) {
        if (result) {
            handleAfterTurn(joinPoint);
        }
    }

    private void handleBeforeTurn(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            val player = playerService.getPlayer(chatId);
            playerService.updatePlayer(effectService.updatePlayerEffects(player));
//            if (args.length > 1 && args[0] instanceof CallbackType callbackType) {
//                if (Set.of(FORWARD, LEFT, RIGHT, BACK).contains(callbackType)) {
//                    val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
//                    if (currentRoom.getRoomContent() instanceof  MonsterRoom monsterRoom) {
//                        val monster = monsterRoom.getMonster();
//                        if (isNull(monster.getAttackPattern()) || monster.getAttackPattern().isEmpty()) {
//                            monster.setAttackPattern(monster.getDefaultAttackPattern());
//                            monster.setCurrentAttack(monster.getAttackPattern().poll());
//                            monsterService.saveOrUpdateMonster(monster);
//                        }
//                        roomService.saveOrUpdateRoomContent(monsterRoom);
//                    }
//                }
//            }
        }
    }

    private void handleAfterTurn(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            val player = playerService.getPlayer(chatId);
            if (player.getHp() < 1) {
                levelService.remove(chatId);
                player.getInventory().clear();
                itemService.dropCollection(chatId);
                player.getEffects().clear();
                playerService.updatePlayer(player);
                messageService.sendDeathMessage(chatId);
                return;
            }
            var playerLevel = player.getPlayerLevel();
            val playerXp = player.getXp();
            if (PlayerLevelService.getLevel(playerXp) > playerLevel) {
                playerLevel = PlayerLevelService.getLevel(playerXp);
                player.setPlayerLevel(playerLevel);
                log.debug("Level {} achieved!", playerLevel);
                player.refillHp();
                player.refillMana();
                player.setNextLevelXp(PlayerLevelService.calculateXPForLevel(playerLevel + 1));
                messageService.sendLevelUpgradeMessage(chatId, player);
            }
            if (roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId()).getRoomContent() instanceof MonsterRoom monsterRoom) {
                monsterService.saveOrUpdateMonster(effectService.updateMonsterEffects(monsterRoom.getMonster()));
            }
        }
    }
}
