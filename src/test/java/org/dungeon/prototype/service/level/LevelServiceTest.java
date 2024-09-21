package org.dungeon.prototype.service.level;

import lombok.val;
import org.dungeon.prototype.TestData;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.document.level.LevelDocument;
import org.dungeon.prototype.model.document.room.RoomContentDocument;
import org.dungeon.prototype.model.document.room.RoomDocument;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.model.effect.ExpirableAdditionEffect;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.*;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.LevelMap;
import org.dungeon.prototype.repository.LevelRepository;
import org.dungeon.prototype.repository.projections.LevelNumberProjection;
import org.dungeon.prototype.service.BaseServiceUnitTest;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.level.generation.LevelGenerationService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.util.LevelUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.test.context.ActiveProfiles;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static org.dungeon.prototype.TestData.getPlayer;
import static org.dungeon.prototype.model.Direction.E;
import static org.dungeon.prototype.model.Direction.N;
import static org.dungeon.prototype.model.Direction.S;
import static org.dungeon.prototype.model.Direction.W;
import static org.dungeon.prototype.model.room.RoomType.TREASURE_LOOTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
class LevelServiceTest extends BaseServiceUnitTest {

    @InjectMocks
    private LevelService levelService;
    @Mock
    private LevelGenerationService levelGenerationService;
    @Mock
    private RoomService roomService;
    @Mock
    private PlayerService playerService;
    @Mock
    private EffectService effectService;
    @Mock
    private LevelRepository levelRepository;
    @Mock
    private MessageService messageService;

    @Test
    @DisplayName("Successfully starts new game")
    void startNewGame() {
        val player = getPlayer(CHAT_ID);
        val level = TestData.getLevel();
        when(levelGenerationService.generateLevel(CHAT_ID, player, 1)).thenReturn(level);

        val document = new LevelDocument();
        val start = new RoomDocument();
        start.setPoint(new Point(5, 6));
        start.setAdjacentRooms(new EnumMap<>(Map.of(
                N, true,
                E, false,
                S, false,
                W, false)));
        document.setStart(start);

        when(levelRepository.existsByChatId(CHAT_ID)).thenReturn(false);
        when(levelRepository.save(any(LevelDocument.class))).thenReturn(document);
        when(messageService.sendRoomMessage(CHAT_ID, player, level.getStart())).thenReturn(true);

        val actualResult = levelService.startNewGame(CHAT_ID, player);

        verify(levelGenerationService).generateLevel(CHAT_ID, player, 1);
        verify(messageService).sendRoomMessage(CHAT_ID, player, level.getStart());

        assertTrue(actualResult);
    }

    @Test
    @DisplayName("Successfully proceeds to next level")
    void nextLevel() {
        val player = getPlayer(CHAT_ID);
        val level = TestData.getLevel();
        val levelNumber = new LevelNumberProjection();
        levelNumber.setNumber(1);
        when(levelRepository.findNumberByChatId(CHAT_ID)).thenReturn(Optional.of(levelNumber));
        when(levelGenerationService.generateLevel(CHAT_ID, player, 2)).thenReturn(level);

        val document = new LevelDocument();
        val start = new RoomDocument();
        start.setPoint(new Point(5, 6));
        start.setAdjacentRooms(new EnumMap<>(Map.of(
                N, true,
                E, false,
                S, false,
                W, false)));
        document.setStart(start);
        when(levelRepository.save(any(LevelDocument.class))).thenReturn(document);
        when(effectService.updateArmorEffect(player)).thenReturn(player);
        when(messageService.sendRoomMessage(CHAT_ID, player, level.getStart())).thenReturn(true);

        val actualResult = levelService.nextLevel(CHAT_ID, player);

        assertTrue(actualResult);
    }

    @Test
    @DisplayName("Successfully continues game")
    void continueGame() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        val currentRoom = mock(Room.class);

        val levelNumber = new LevelNumberProjection();
        levelNumber.setNumber(1);
        when(levelRepository.findNumberByChatId(CHAT_ID)).thenReturn(Optional.of(levelNumber));
        when(messageService.sendRoomMessage(CHAT_ID, player, currentRoom)).thenReturn(true);

        val actualResult = levelService.continueGame(CHAT_ID, player, currentRoom);

        verify(messageService).sendRoomMessage(CHAT_ID, player, currentRoom);
        assertTrue(actualResult);
    }

    @Test
    @DisplayName("Successfully moves to adjacent room")
    void moveToRoom() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID);
        GridSection[][] levelGrid = new GridSection[7][7];
        levelGrid[5][6] = new GridSection(5, 6);
        levelGrid[6][6] = new GridSection(6, 6);
        val newPoint = new Point(5, 6);
        val nextRoom = new Room();
        nextRoom.setPoint(newPoint);
        val levelMap = mock(LevelMap.class);
        when(levelMap.isContainsRoom(5, 6)).thenReturn(false);
        when(levelMap.addRoom(levelGrid[5][6])).thenReturn(true);
        val document = new LevelDocument();
        val newRoomDocument = new RoomDocument();
        newRoomDocument.setPoint(new Point(5, 6));
        val oldRoom = new RoomDocument();
        oldRoom.setPoint(new Point(6, 6));
        newRoomDocument.setAdjacentRooms(new EnumMap<>(Map.of(
                N, false,
                W, false,
                E, true,
                S, false
        )));
        document.setRoomsMap(Map.of("{\"x\":5, \"y\":6}", newRoomDocument,
                "{\"x\":6, \"y\":6}", oldRoom));
        document.setLevelMap(levelMap);
        document.setGrid(levelGrid);

        when(levelRepository.findByChatId(CHAT_ID)).thenReturn(Optional.of(document));

        when(levelRepository.save(any(LevelDocument.class))).thenReturn(document);
        when(playerService.updatePlayer(player)).thenReturn(player);

        ArgumentCaptor<Room> roomArgumentCaptor = ArgumentCaptor.forClass(Room.class);
        when(messageService.sendRoomMessage(eq(CHAT_ID), eq(player), roomArgumentCaptor.capture())).thenReturn(true);

        Room newRoom = new Room();
        newRoom.setPoint(new Point(5,6));
        newRoom.setChatId(CHAT_ID);
        val actualResult = levelService.moveToRoom(CHAT_ID, player, newRoom, S);
        val actualRoomValue = roomArgumentCaptor.getValue();

        assertEquals(newRoom.getChatId(), actualRoomValue.getChatId());
        assertEquals(newRoom.getPoint(), actualRoomValue.getPoint());
        assertTrue(actualResult);
    }

    @Test
    @DisplayName("Successfully performs updates required after monster kill")
    void updateAfterMonsterKill() {
        val room = new Room();
        GridSection[][] levelGrid = new GridSection[7][7];
        levelGrid[5][5] = new GridSection(5, 5);
        room.setChatId(CHAT_ID);
        room.setPoint(new Point(5, 5));
        val roomContent = new MonsterRoom();
        val monster = Monster.builder()
                .monsterClass(MonsterClass.VAMPIRE)
                .hp(0)
                .build();
        roomContent.setMonster(monster);
        room.setRoomContent(roomContent);
        LevelDocument document = new LevelDocument();
        RoomDocument roomDocument = new RoomDocument();
        RoomContentDocument roomContentDocument = new RoomContentDocument();
        roomContentDocument.setRoomType(RoomType.VAMPIRE);
        roomDocument.setRoomContent(roomContentDocument);
        val roomsMap = Map.of("{\"x\":5,\"y\":5}", roomDocument);
        document.setChatId(CHAT_ID);
        document.setGrid(levelGrid);
        document.setRoomsMap(roomsMap);
        val updatedLevel = new LevelDocument();
        val updatedRoomDocument = new RoomDocument();
        val updatedRoomContent = new RoomContentDocument();
        updatedRoomContent.setRoomType(RoomType.VAMPIRE_KILLED);
        updatedRoomDocument.setRoomContent(updatedRoomContent);
        val updatedRoomsMap =  Map.of("{\"x\":5,\"y\":5}", updatedRoomDocument);
        updatedLevel.setRoomsMap(updatedRoomsMap);
        val updatedRoom = new Room();
        updatedRoom.setId(CURRENT_ROOM_ID);
        updatedRoom.setRoomContent(new EmptyRoom(RoomType.VAMPIRE_KILLED));
        when(roomService.saveOrUpdateRoomContent(any(RoomContent.class))).thenReturn(new EmptyRoom(RoomType.VAMPIRE_KILLED));
        when(roomService.saveOrUpdateRoom(any(Room.class))).thenReturn(updatedRoom);
        when(levelRepository.findByChatId(CHAT_ID)).thenReturn(Optional.of(document));
        when(levelRepository.save(any())).thenReturn(updatedLevel);

        levelService.updateAfterMonsterKill(room);

        ArgumentCaptor<LevelDocument> updatedLevelCaptor = ArgumentCaptor.forClass(LevelDocument.class);
        verify(levelRepository).save(updatedLevelCaptor.capture());
        val actualValue = updatedLevelCaptor.getValue();
        assertEquals(document.getChatId(), actualValue.getChatId());
        assertEquals(RoomType.VAMPIRE_KILLED, actualValue.getRoomsMap().get("{\"x\":5,\"y\":5}").getRoomContent().getRoomType());
    }

    @Test
    @DisplayName("Successfully performs updates required after looting treasure")
    void updateAfterTreasureLooted() {
        val room = new Room();
        room.setId(CURRENT_ROOM_ID);
        room.setChatId(CHAT_ID);
        room.setPoint(new Point(5, 5));
        val roomContent = new Treasure();
        roomContent.setGold(0);
        roomContent.setItems(new HashSet<>());
        room.setRoomContent(roomContent);

        val level = new LevelDocument();
        GridSection[][] grid = new GridSection[6][6];
        grid[5][5] = new GridSection(5, 5);
        level.setGrid(grid);
        val roomDocument = new RoomDocument();
        roomDocument.setId(CURRENT_ROOM_ID);
        val roomsMap = Map.of("{\"x\":5,\"y\":5}", roomDocument);
        level.setRoomsMap(roomsMap);

        when(levelRepository.findByChatId(CHAT_ID)).thenReturn(Optional.of(level));

        levelService.updateAfterTreasureLooted(room);

        ArgumentCaptor<Room> roomArgumentCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomService).saveOrUpdateRoom(roomArgumentCaptor.capture());
        val updatedRoom = roomArgumentCaptor.getValue();
        assertEquals(room.getId(), updatedRoom.getId());
        assertEquals(room.getChatId(), updatedRoom.getChatId());
        assertEquals(room.getPoint(), updatedRoom.getPoint());
        assertEquals(TREASURE_LOOTED, updatedRoom.getRoomContent().getRoomType());

        ArgumentCaptor<LevelDocument> levelDocumentArgumentCaptor = ArgumentCaptor.forClass(LevelDocument.class);
        verify(levelRepository).save(levelDocumentArgumentCaptor.capture());
        val updatedLevel = levelDocumentArgumentCaptor.getValue();
        assertEquals(level.getChatId(), updatedLevel.getChatId());
    }

    @Test
    @DisplayName("Successfully performs shrine usage")
    void shrineRefill() {
        val player = getPlayer(CHAT_ID, CURRENT_ROOM_ID, 5, 20);
        val level = new Level();
        GridSection[][] grid = new GridSection[6][6];
        grid[5][5] = new GridSection(5, 5);
        level.setGrid(grid);
        val room = new Room();
        val roomContent = new HealthShrine();
        ExpirableAdditionEffect effect = ExpirableAdditionEffect.builder()
                .attribute(EffectAttribute.HEALTH)
                .isAccumulated(true)
                .turnsLeft(3)
                .amount(10)
                .build();
        roomContent.setEffect(effect);
        room.setRoomContent(roomContent);
        room.setId(CURRENT_ROOM_ID);
        room.setPoint(new Point(5, 5));
        level.setRoomsMap(Map.of(new Point(5, 5), room));

        levelService.shrineRefill(CHAT_ID, player, room, level);

        verify(messageService).sendRoomMessage(CHAT_ID, player, room);
        assertTrue(player.getEffects().contains(effect));
    }

    @Test
    @DisplayName("Successfully saves level to repository")
    void saveOrUpdateLevel() {
        val level = mock(Level.class);
        levelService.saveOrUpdateLevel(level);

        verify(levelRepository).save(any(LevelDocument.class));
    }

    @Test
    @DisplayName("Successfully send map message")
    void sendOrUpdateMapMessage() {
        Player player = new Player();
        Point position = new Point(5, 5);
        player.setCurrentRoom(position);
        player.setDirection(N);
        LevelDocument level = new LevelDocument();
        GridSection[][] grid = new GridSection[6][6];
        level.setGrid(grid);
        LevelMap levelMap = new LevelMap();
        level.setLevelMap(levelMap);

        try (MockedStatic<LevelUtil> levelUtilMockedStatic = mockStatic(LevelUtil.class)){
            levelUtilMockedStatic.when(() -> LevelUtil.printMap(grid, levelMap, position, N)).thenReturn("mapString");
            when(levelRepository.findByChatId(CHAT_ID)).thenReturn(Optional.of(level));
            levelService.sendOrUpdateMapMessage(CHAT_ID , player);

            verify(messageService).sendMapMenuMessage(CHAT_ID, "mapString");
        }
    }

    @Test
    @DisplayName("Successfully loads level from repository")
    void getLevel() {
        levelService.getLevel(CHAT_ID);

        verify(levelRepository).findByChatId(CHAT_ID);
    }

    @Test
    @DisplayName("Successfully removes level from repository")
    void remove() {
        levelService.remove(CHAT_ID);

        verify(levelRepository).removeByChatId(CHAT_ID);
    }

    @Test
    @DisplayName("Successfully checks if level present in repository")
    void hasLevel() {
        when(levelRepository.existsByChatId(CHAT_ID)).thenReturn(true);

        val actualResult = levelService.hasLevel(CHAT_ID);

        assertTrue(actualResult);
    }
}