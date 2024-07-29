package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.room.RoomType;

@Slf4j
@UtilityClass
public class FileUtil {

    private static final String DEFAULT_ROOM_ASSET = "static/images/room/default.png";
    private static final String WEREWOLF_ROOM_ASSET = "static/images/room/monster/werewolf.png";
    private static final String WEREWOLF_KILLED_ROOM_ASSET = "static/images/room/monster/werewolf_killed.png";
    private static final String VAMPIRE_ROOM_ASSET = "static/images/room/monster/vampire.png";
    private static final String VAMPIRE_KILLED_ROOM_ASSET = "static/images/room/monster/vampire_killed.png";
    private static final String SWAMP_BEAST_ROOM_ASSET = "static/images/room/monster/swamp_beast.png";
    private static final String SWAMP_BEAST_KILLED_ROOM_ASSET = "static/images/room/monster/swamp_beast_killed.png";
    private static final String DRAGON_ROOM_ASSET = "static/images/room/monster/dragon.png";
    private static final String DRAGON_KILLED_ROOM_ASSET = "static/images/room/monster/dragon_killed.png";
    private static final String ZOMBIE_ROOM_ASSET = "static/images/room/monster/zombie.png";
    private static final String ZOMBIE_KILLED_ROOM_ASSET = "static/images/room/monster/zombie_killed.png";
    private static final String TREASURE_ROOM_ASSET = "static/images/room/treasure.png";
    private static final String TREASURE_LOOTED_ROOM_ASSET = "static/images/room/treasure_looted.png";
    private static final String SHRINE_HEALTH_ROOM_ASSET = "static/images/room/shrine_health.png";
    private static final String SHRINE_MANA_ROOM_ASSET = "static/images/room/shrine_mana.png";
    private static final String SHRINE_DRAINED_ROOM_ASSET = "static/images/room/shrine_drained.png";
    private static final String MERCHANT_ROOM_ASSET = "static/images/room/merchant.png";

    public static String getRoomAsset(RoomType roomType) {
        return switch (roomType) {
            case WEREWOLF -> WEREWOLF_ROOM_ASSET;
            case WEREWOLF_KILLED -> WEREWOLF_KILLED_ROOM_ASSET;
            case VAMPIRE -> VAMPIRE_ROOM_ASSET;
            case VAMPIRE_KILLED -> VAMPIRE_KILLED_ROOM_ASSET;
            case SWAMP_BEAST -> SWAMP_BEAST_ROOM_ASSET;
            case SWAMP_BEAST_KILLED -> SWAMP_BEAST_KILLED_ROOM_ASSET;
            case DRAGON -> DRAGON_ROOM_ASSET;
            case DRAGON_KILLED -> DRAGON_KILLED_ROOM_ASSET;
            case ZOMBIE -> ZOMBIE_ROOM_ASSET;
            case ZOMBIE_KILLED -> ZOMBIE_KILLED_ROOM_ASSET;
            case TREASURE -> TREASURE_ROOM_ASSET;
            case TREASURE_LOOTED -> TREASURE_LOOTED_ROOM_ASSET;
            case HEALTH_SHRINE -> SHRINE_HEALTH_ROOM_ASSET;
            case MANA_SHRINE -> SHRINE_MANA_ROOM_ASSET;
            case SHRINE_DRAINED -> SHRINE_DRAINED_ROOM_ASSET;
            case MERCHANT -> MERCHANT_ROOM_ASSET;
            default -> DEFAULT_ROOM_ASSET;
        };
    }
}
