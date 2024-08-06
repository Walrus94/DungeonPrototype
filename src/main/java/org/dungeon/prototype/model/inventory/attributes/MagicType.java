package org.dungeon.prototype.model.inventory.attributes;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MagicType implements EnumAttribute {
    FIRE("Fire"),
    WATER("Water"),
    LIFE("Life"),
    DEATH("Death");

    private final String value;

    MagicType(String value) {
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
