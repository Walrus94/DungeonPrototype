package org.dungeon.prototype.model.inventory.attributes;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MagicType {
    FIRE("Fire"),
    WATER("Water"),
    LIFE("Life"),
    DEATH("Death");

    private final String value;

    MagicType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
