package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import org.dungeon.prototype.properties.CallbackType;

@UtilityClass
public class MessageUtil {
    //TODO: get rid of it
    public static String formatItemType(CallbackType equippedType) {
        return switch (equippedType) {
            case VEST -> "Vest";
            case GLOVES -> "Gloves";
            case BOOTS -> "Boots";
            case HEAD -> "Head";
            case LEFT_HAND -> "Secondary weapon";
            case RIGHT_HAND -> "Primary weapon";
            default -> "Item";
        };
    }
}
