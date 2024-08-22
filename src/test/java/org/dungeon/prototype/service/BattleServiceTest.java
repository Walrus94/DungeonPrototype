package org.dungeon.prototype.service;

import lombok.val;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial;
import org.dungeon.prototype.model.monster.MonsterAttackType;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.properties.BattleProperties;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.util.RandomUtil;
import org.dungeon.prototype.util.RoomGenerationUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.dungeon.prototype.TestUtils.getLevel;
import static org.dungeon.prototype.TestUtils.getMonster;
import static org.dungeon.prototype.TestUtils.getPlayer;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.SLASH;
import static org.dungeon.prototype.model.room.RoomType.ZOMBIE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class BattleServiceTest extends BaseServiceUnitTest {
    @Mock
    private BattleProperties battleProperties;
    @Mock
    private PlayerService playerService;
    @Mock
    private MonsterService monsterService;
    @Mock
    private LevelService levelService;
    @Mock
    private RoomService roomService;
    @InjectMocks
    private BattleService battleService;

    @Test
    @DisplayName("Successfully process player attacking monster and vice versa")
    public void attack() {
        try (MockedStatic<RoomGenerationUtils> roomGenerationUtilsMock = Mockito.mockStatic(RoomGenerationUtils.class)) {
            roomGenerationUtilsMock.when(RoomGenerationUtils::getMonsterRoomTypes).thenReturn(List.of(RoomType.WEREWOLF, RoomType.VAMPIRE, RoomType.SWAMP_BEAST, RoomType.ZOMBIE, RoomType.DRAGON));
            val level = getLevel();
            val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
            val monster = getMonster();
            val currentRoom = mock(Room.class);
            val roomContent = mock(MonsterRoom.class);
            when(currentRoom.getRoomContent()).thenReturn(roomContent);
            when(roomContent.getMonster()).thenReturn(monster);
            when(roomContent.getRoomType()).thenReturn(ZOMBIE);

            when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
            when(levelService.getLevel(CHAT_ID)).thenReturn(level);
            when(roomService.getRoomByIdAndChatId(CHAT_ID, CURRENT_ROOM_ID)).thenReturn(currentRoom);
            try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)){
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

                battleService.attack(CHAT_ID, CallbackType.ATTACK);

                verify(playerService).updatePlayer(player);
                verify(monsterService).saveOrUpdateMonster(monster);
                assertEquals(15, player.getHp());
                assertEquals(1, monster.getHp());
            }
        }
    }
}