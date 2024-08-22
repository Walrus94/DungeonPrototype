package org.dungeon.prototype.service;

import org.dungeon.prototype.BaseUnitTest;
import org.dungeon.prototype.aspect.AspectProcessor;
import org.dungeon.prototype.aspect.MessagingAspectProcessor;
import org.dungeon.prototype.repository.ArmorSetRepository;
import org.dungeon.prototype.repository.InventoryRepository;
import org.dungeon.prototype.repository.ItemRepository;
import org.dungeon.prototype.repository.LevelRepository;
import org.dungeon.prototype.repository.PlayerRepository;
import org.dungeon.prototype.repository.RoomContentRepository;
import org.dungeon.prototype.repository.RoomRepository;
import org.dungeon.prototype.repository.WeaponSetRepository;
import org.springframework.boot.test.mock.mockito.MockBean;

public class BaseServiceUnitTest extends BaseUnitTest {
    protected static final Long CHAT_ID = 123456789L;
    protected static final String CURRENT_ROOM_ID = "room_id";
    @MockBean
    AspectProcessor aspectProcessor;
    @MockBean
    MessagingAspectProcessor messagingAspectProcessor;
    @MockBean
    private PlayerRepository playerRepository;
    @MockBean
    private LevelRepository levelRepository;
    @MockBean
    private RoomRepository roomRepository;
    @MockBean
    private RoomContentRepository roomContentRepository;
    @MockBean
    private ItemRepository itemRepository;
    @MockBean
    private InventoryRepository inventoryRepository;
    @MockBean
    private ArmorSetRepository armorSetRepository;
    @MockBean
    private WeaponSetRepository weaponSetRepository;
}
