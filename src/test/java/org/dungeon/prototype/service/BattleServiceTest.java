package org.dungeon.prototype.service;

import lombok.val;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.service.balancing.BalanceMatrixService;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.MonsterService;
import org.dungeon.prototype.util.RandomUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.test.context.ActiveProfiles;


import static org.dungeon.prototype.TestData.getMonster;
import static org.dungeon.prototype.TestData.getPlayer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
class BattleServiceTest extends BaseServiceUnitTest {
    @Mock
    private PlayerService playerService;
    @Mock
    private LevelService levelService;
    @Mock
    private MonsterService monsterService;
    @Mock
    private BalanceMatrixService balanceMatrixService;
    @Mock
    private MessageService messageService;
    @InjectMocks
    private BattleService battleService;

    @Test
    @DisplayName("Successfully process player attacking monster and vice versa")
    public void attack() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID, 20, 30);
        val monster = getMonster(10, 15);
        val currentRoom = mock(Room.class);
        val roomContent = mock(MonsterRoom.class);
        when(currentRoom.getRoomContent()).thenReturn(roomContent);
        when(roomContent.getMonster()).thenReturn(monster);

        try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)) {
            randomUtilMock.when(() -> RandomUtil.flipAdjustedCoin(1.0)).thenReturn(true);
            randomUtilMock.when(() -> RandomUtil.flipAdjustedCoin(0.0)).thenReturn(false);

            when(balanceMatrixService.getBalanceMatrixValue(CHAT_ID, "player_attack", 0,0)).thenReturn(1.0);

            doNothing().when(messageService).sendMonsterRoomMessage(CHAT_ID, player, currentRoom);

            battleService.attack(CHAT_ID, player, currentRoom, CallbackType.ATTACK);

            verify(playerService).updatePlayer(player);
            assertEquals(15, player.getHp());
            assertEquals(5, monster.getHp());
            verify(monsterService).updateMonster(monster);
            verify(messageService).sendMonsterRoomMessage(CHAT_ID, player, currentRoom);
        }
    }

    @Test
    @DisplayName("Successfully process player attacking monster and killing it")
    public void attack_monsterKilled() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID, 20, 30);
        val monster = getMonster(1, 15);
        val currentRoom = mock(Room.class);
        val roomContent = mock(MonsterRoom.class);
        when(currentRoom.getRoomContent()).thenReturn(roomContent);
        when(roomContent.getMonster()).thenReturn(monster);

        try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)) {
            randomUtilMock.when(() -> RandomUtil.flipAdjustedCoin(1.0)).thenReturn(true);
            randomUtilMock.when(() -> RandomUtil.flipAdjustedCoin(0.0)).thenReturn(false);

            when(balanceMatrixService.getBalanceMatrixValue(CHAT_ID, "player_attack", 0,0)).thenReturn(1.1);
            doNothing().when(levelService).updateAfterMonsterKill(eq(currentRoom));
            doNothing().when(messageService).sendRoomMessage(CHAT_ID, player, currentRoom);

            battleService.attack(CHAT_ID, player, currentRoom, CallbackType.ATTACK);

            verify(playerService).updatePlayer(player);
            verify(levelService).updateAfterMonsterKill(eq(currentRoom));
            assertEquals(20, player.getHp());
            verify(messageService).sendRoomMessage(CHAT_ID, player, currentRoom);
        }
    }

    @Test
    @DisplayName("Successfully process player attacking monster and monster killing player")
    public void attack_playerKilled() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID, 5, 30);
        val monster = getMonster(15, 15);
        val currentRoom = mock(Room.class);
        val roomContent = mock(MonsterRoom.class);
        when(currentRoom.getRoomContent()).thenReturn(roomContent);
        when(roomContent.getMonster()).thenReturn(monster);

        try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)) {
            randomUtilMock.when(() -> RandomUtil.flipAdjustedCoin(1.0)).thenReturn(true);
            randomUtilMock.when(() -> RandomUtil.flipAdjustedCoin(0.0)).thenReturn(false);

            when(balanceMatrixService.getBalanceMatrixValue(CHAT_ID, "player_attack", 0,0)).thenReturn(1.1);

            doNothing().when(messageService).sendMonsterRoomMessage(CHAT_ID, player, currentRoom);

            battleService.attack(CHAT_ID, player, currentRoom, CallbackType.ATTACK);

            verify(playerService).updatePlayer(player);
            assertEquals(0, player.getHp());
            verify(messageService).sendMonsterRoomMessage(CHAT_ID, player, currentRoom);
        }
    }
}