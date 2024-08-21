package org.dungeon.prototype.service;

import lombok.val;
import org.dungeon.prototype.BaseUnitTest;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.WeaponSet;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.properties.BattleProperties;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.util.RoomGenerationUtils;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.SLASH;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponType.SWORD;
import static org.dungeon.prototype.model.room.RoomType.ZOMBIE;
import static org.mockito.Mockito.*;


class BattleServiceTest extends BaseUnitTest {

    private static final Long CHAT_ID = 123456789L;
    private static final String CURRENT_ROOM_ID = "room_id";
    @Mock
    private BattleProperties battleProperties;
    @Mock
    private PlayerService playerService;
    @Mock
    private LevelService levelService;
    @Mock
    private RoomService roomService;
    @InjectMocks
    private BattleService battleService;


    @Test
    public void attack() {
        try (MockedStatic<RoomGenerationUtils> roomGenerationUtilsMock = Mockito.mockStatic(RoomGenerationUtils.class)) {
            roomGenerationUtilsMock.when(RoomGenerationUtils::getMonsterRoomTypes).thenReturn(List.of(RoomType.WEREWOLF, RoomType.VAMPIRE, RoomType.SWAMP_BEAST, RoomType.ZOMBIE, RoomType.DRAGON));
            val player = mock(Player.class);
            val inventory = mock(Inventory.class);
            val monster = mock(Monster.class);
            val currentRoom = mock(Room.class);
            val level = mock(Level.class);
            val roomContent = mock(MonsterRoom.class);
            when(player.getCurrentRoomId()).thenReturn(CURRENT_ROOM_ID);
            when(player.getInventory()).thenReturn(inventory);
            when(player.getAttributes()).thenReturn(new EnumMap<>(Map.of(PlayerAttribute.POWER, 5, PlayerAttribute.MAGIC, 2)));
            WeaponSet weaponSet = mock(WeaponSet.class);
            when(inventory.getWeaponSet()).thenReturn(weaponSet);
            Weapon weapon = mock(Weapon.class);
            when(weaponSet.getPrimaryWeapon()).thenReturn(weapon);
            WeaponAttributes weaponAttributes = new WeaponAttributes();
            weaponAttributes.setWeaponType(SWORD);
            weaponAttributes.setWeaponAttackType(SLASH);
            when(weapon.getAttributes()).thenReturn(weaponAttributes);
            when(currentRoom.getRoomContent()).thenReturn(roomContent);
            when(roomContent.getMonster()).thenReturn(monster);
            when(roomContent.getRoomType()).thenReturn(ZOMBIE);

            when(monster.getMonsterClass()).thenReturn(MonsterClass.ZOMBIE);

            when(playerService.getPlayer(CHAT_ID)).thenReturn(player);
            when(levelService.getLevel(CHAT_ID)).thenReturn(level);
            when(roomService.getRoomByIdAndChatId(CHAT_ID, CURRENT_ROOM_ID)).thenReturn(currentRoom);

            val monsterDefenseRatioMap = mock(BattleProperties.MonsterDefenseRatioMap.class);
            when(monsterDefenseRatioMap.getMonsterDefenseRatioMap()).thenReturn(Map.of(MonsterClass.ZOMBIE, 0.9));
            val monsterDefenseRatioMatrix = Map.of(SLASH, monsterDefenseRatioMap);
            when(battleProperties.getMonsterDefenseRatioMatrix()).thenReturn(monsterDefenseRatioMatrix);

            battleService.attack(CHAT_ID, true);

            verify(playerService).updatePlayer(player);
        }
    }
}