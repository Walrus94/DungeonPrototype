package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.exception.CallbackParsingException;
import org.dungeon.prototype.exception.FileLoadingException;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.properties.CallbackType;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

import static org.dungeon.prototype.model.Direction.E;
import static org.dungeon.prototype.model.Direction.N;
import static org.dungeon.prototype.model.Direction.S;
import static org.dungeon.prototype.model.Direction.W;
import static org.dungeon.prototype.properties.CallbackType.FORWARD;
import static org.dungeon.prototype.properties.CallbackType.LEFT;
import static org.dungeon.prototype.properties.CallbackType.RIGHT;

@Slf4j
@UtilityClass
public class FileUtil {

    private static final String DEFAULT_BACKGROUND_ASSET = "static/images/room/default_background.png";
    private static final String WEREWOLF_ROOM_ASSET = "static/images/room/content/monster/werewolf.png";
    private static final String MONSTER_KILLED_ROOM_ASSET = "static/images/room/content/monster/monster_killed.png";
    private static final String VAMPIRE_ROOM_ASSET = "static/images/room/content/monster/vampire.png";
    private static final String SWAMP_BEAST_ROOM_ASSET = "static/images/room/content/monster/swamp_beast.png";
    private static final String DRAGON_ROOM_ASSET = "static/images/room/content/monster/dragon.png";
    private static final String ZOMBIE_ROOM_ASSET = "static/images/room/content/monster/zombie.png";
    private static final String TREASURE_ROOM_ASSET = "static/images/room/content/treasure.png";
    private static final String TREASURE_LOOTED_ROOM_ASSET = "static/images/room/content/treasure_looted.png";
    private static final String SHRINE_HEALTH_ROOM_ASSET = "static/images/room/content/health_shrine.png";
    private static final String SHRINE_MANA_ROOM_ASSET = "static/images/room/content/mana_shrine.png";
    private static final String SHRINE_DRAINED_ROOM_ASSET = "static/images/room/content/shrine_drained.png";
    private static final String MERCHANT_ROOM_ASSET = "static/images/room/content/merchant.png";
    private static final String ANVIL_ROOM_ASSET = "static/images/room/content/anvil.png";
    private static final String EMPTY_LAYER_ASSET = "static/images/room/content/empty.png";
    private static final String LEFT_DOOR_ASSET = "static/images/room/door/left.png";
    private static final String RIGHT_DOOR_ASSET = "static/images/room/door/right.png";
    private static final String FORWARD_DOOR_ASSET = "static/images/room/door/forward.png";


    public static BufferedImage getBackgroundLayer(long chatId) {
        ClassPathResource imgFile = new ClassPathResource(DEFAULT_BACKGROUND_ASSET);
        try (InputStream inputStream = imgFile.getInputStream()){
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            throw new FileLoadingException(chatId, e.getMessage());
        }
    }

    public static BufferedImage getDoorLayerFragment(long chatId, CallbackType doorType) {
        ClassPathResource imgFile = new ClassPathResource(switch (doorType) {
            case LEFT -> LEFT_DOOR_ASSET;
            case RIGHT -> RIGHT_DOOR_ASSET;
            case FORWARD -> FORWARD_DOOR_ASSET;
            default -> throw new CallbackParsingException(doorType.toString());
        });
        try (InputStream inputStream = imgFile.getInputStream()){
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            throw new FileLoadingException(chatId, e.getMessage());
        }
    }

    public static BufferedImage getRoomContentLayer(long chatId, String asset) {
        ClassPathResource imgFile = new ClassPathResource(asset);
        try (InputStream inputStream = imgFile.getInputStream()){
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            throw new FileLoadingException(chatId, e.getMessage());
        }
    }

    public static EnumMap<CallbackType, Boolean> getAdjacentRoomMap(EnumMap<Direction, Boolean> adjacentRooms, Direction direction) {
        return switch (direction) {
            case N -> new EnumMap<>(Map.of(LEFT, adjacentRooms.get(W),
                    FORWARD, adjacentRooms.get(N),
                    RIGHT, adjacentRooms.get(E)));
            case E -> new EnumMap<>(Map.of(LEFT, adjacentRooms.get(N),
                    FORWARD, adjacentRooms.get(E),
                    RIGHT, adjacentRooms.get(S)));
            case S -> new EnumMap<>(Map.of(LEFT, adjacentRooms.get(E),
                    FORWARD, adjacentRooms.get(S),
                    RIGHT, adjacentRooms.get(W)));
            case W -> new EnumMap<>(Map.of(LEFT, adjacentRooms.get(S),
                    FORWARD, adjacentRooms.get(W),
                    RIGHT, adjacentRooms.get(N)));
        };
    }

    public static String getRoomAsset(RoomType roomType) {
        return switch (roomType) {
            case WEREWOLF -> WEREWOLF_ROOM_ASSET;
            case WEREWOLF_KILLED, VAMPIRE_KILLED, SWAMP_BEAST_KILLED, DRAGON_KILLED, ZOMBIE_KILLED -> MONSTER_KILLED_ROOM_ASSET;
            case VAMPIRE -> VAMPIRE_ROOM_ASSET;
            case SWAMP_BEAST -> SWAMP_BEAST_ROOM_ASSET;
            case DRAGON -> DRAGON_ROOM_ASSET;
            case ZOMBIE -> ZOMBIE_ROOM_ASSET;
            case TREASURE -> TREASURE_ROOM_ASSET;
            case TREASURE_LOOTED -> TREASURE_LOOTED_ROOM_ASSET;
            case HEALTH_SHRINE -> SHRINE_HEALTH_ROOM_ASSET;
            case MANA_SHRINE -> SHRINE_MANA_ROOM_ASSET;
            case SHRINE_DRAINED -> SHRINE_DRAINED_ROOM_ASSET;
            case MERCHANT -> MERCHANT_ROOM_ASSET;
            case ANVIL -> ANVIL_ROOM_ASSET;
            default -> EMPTY_LAYER_ASSET;
        };
    }
}
