package org.dungeon.prototype.model.inventory.attributes.wearable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum WearableType implements EnumAttribute {
    HELMET("Helmet"),
    VEST("Vest"),
    GLOVES("Gloves"),
    BOOTS("Boots");

    private final String value;

    WearableType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static WearableType fromValue(String value) {
        for (WearableType e : WearableType.values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown attribute: " + value);
    }

    @JsonValue
    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
