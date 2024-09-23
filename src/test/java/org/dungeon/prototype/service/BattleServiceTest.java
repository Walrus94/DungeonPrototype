package org.dungeon.prototype.service;

import lombok.val;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial;
import org.dungeon.prototype.model.monster.MonsterAttackType;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.properties.BattleProperties;
import org.dungeon.prototype.properties.CallbackType;
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

import java.util.Map;

import static org.dungeon.prototype.TestData.getMonster;
import static org.dungeon.prototype.TestData.getPlayer;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.SLASH;
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
    private BattleProperties battleProperties;
    @Mock
    private PlayerService playerService;
    @Mock
    private LevelService levelService;
    @Mock
    private MonsterService monsterService;
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

            val monsterDefenseRatioMap = mock(BattleProperties.MonsterDefenseRatioMap.class);
            val playerDefenseRatioMap = mock(BattleProperties.MaterialDefenseRatioMap.class);
            when(monsterDefenseRatioMap.getMonsterDefenseRatioMap()).thenReturn(Map.of(MonsterClass.ZOMBIE, 0.9));
            when(playerDefenseRatioMap.getMaterialDefenseRatioMap()).thenReturn(Map.of(WearableMaterial.IRON, 1.1));
            val monsterDefenseRatioMatrix = Map.of(SLASH, monsterDefenseRatioMap);
            val playerDefenseRatioMatrix = Map.of(MonsterAttackType.SLASH, playerDefenseRatioMap);
            when(battleProperties.getMonsterDefenseRatioMatrix()).thenReturn(monsterDefenseRatioMatrix);
            when(battleProperties.getPlayerDefenseRatioMatrix()).thenReturn(playerDefenseRatioMatrix);
            doNothing().when(messageService).sendRoomMessage(CHAT_ID, player, currentRoom);

            battleService.attack(CHAT_ID, player, currentRoom, CallbackType.ATTACK);

            verify(playerService).updatePlayer(player);
            assertEquals(15, player.getHp());
            assertEquals(6, monster.getHp());
            verify(messageService).sendRoomMessage(CHAT_ID, player, currentRoom);
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

            val monsterDefenseRatioMap = mock(BattleProperties.MonsterDefenseRatioMap.class);
            when(monsterDefenseRatioMap.getMonsterDefenseRatioMap()).thenReturn(Map.of(MonsterClass.ZOMBIE, 0.9));
            val monsterDefenseRatioMatrix = Map.of(SLASH, monsterDefenseRatioMap);
            when(battleProperties.getMonsterDefenseRatioMatrix()).thenReturn(monsterDefenseRatioMatrix);
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

            val monsterDefenseRatioMap = mock(BattleProperties.MonsterDefenseRatioMap.class);
            val playerDefenseRatioMap = mock(BattleProperties.MaterialDefenseRatioMap.class);
            when(monsterDefenseRatioMap.getMonsterDefenseRatioMap()).thenReturn(Map.of(MonsterClass.ZOMBIE, 0.9));
            when(playerDefenseRatioMap.getMaterialDefenseRatioMap()).thenReturn(Map.of(WearableMaterial.IRON, 1.1));
            val monsterDefenseRatioMatrix = Map.of(SLASH, monsterDefenseRatioMap);
            val playerDefenseRatioMatrix = Map.of(MonsterAttackType.SLASH, playerDefenseRatioMap);
            when(battleProperties.getMonsterDefenseRatioMatrix()).thenReturn(monsterDefenseRatioMatrix);
            when(battleProperties.getPlayerDefenseRatioMatrix()).thenReturn(playerDefenseRatioMatrix);

            doNothing().when(messageService).sendRoomMessage(CHAT_ID, player, currentRoom);

            battleService.attack(CHAT_ID, player, currentRoom, CallbackType.ATTACK);

            verify(playerService).updatePlayer(player);
            assertEquals(0, player.getHp());
            verify(messageService).sendRoomMessage(CHAT_ID, player, currentRoom);
        }
    }
}