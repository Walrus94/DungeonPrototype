package org.dungeon.prototype.model.inventory.attributes.weapon;

import com.fasterxml.jackson.annotation.JsonValue;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum Handling implements EnumAttribute {
    MAIN("Main"),
    SECONDARY("Secondary"),
    SINGLE_HANDED("Single-handed"),
    TWO_HANDED("Two-handed");
    private final String value;

    Handling(String value) {
        this.value = value;
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
