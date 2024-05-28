package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.room.Room;
import org.springframework.core.io.ClassPathResource;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@UtilityClass
public class FileUtil {

    private static final String DEFAULT_ROOM_ASSET = "static/images/room/default.png";
    private static final String MONSTER_ROOM_ASSET = "static/images/room/monster.png";
    private static final String MONSTER_KILLED_ROOM_ASSET = "static/images/room/monster_killed.png";
    private static final String TREASURE_ROOM_ASSET = "static/images/room/treasure.png";
    private static final String TREASURE_LOOTED_ROOM_ASSET = "static/images/room/treasure_looted.png";
    private static final String SHRINE_ROOM_ASSET = "static/images/room/shrine.png";
    private static final String SHRINE_DRAINED_ROOM_ASSET = "static/images/room/shrine_drained.png";
    private static final String MERCHANT_ROOM_ASSET = "static/images/room/merchant.png";

    public static String getRoomAsset(Room.Type roomType) {
        return switch (roomType) {
            case MONSTER -> MONSTER_ROOM_ASSET;
            case MONSTER_KILLED -> MONSTER_KILLED_ROOM_ASSET;
            case TREASURE -> TREASURE_ROOM_ASSET;
            case TREASURE_LOOTED -> TREASURE_LOOTED_ROOM_ASSET;
            case SHRINE -> SHRINE_ROOM_ASSET;
            case SHRINE_DRAINED -> SHRINE_DRAINED_ROOM_ASSET;
            case MERCHANT -> MERCHANT_ROOM_ASSET;
            default -> DEFAULT_ROOM_ASSET;
        };
    }

    public static InputFile getInputFile(String path) {
        ClassPathResource imgFile = new ClassPathResource(path);
        try (InputStream inputStream = imgFile.getInputStream()) {
            return new InputFile(inputStream, imgFile.getFilename());
        } catch (IOException e) {
            log.error("Error loading file: {}", e.getMessage());
            return null;
        }
    }
}
