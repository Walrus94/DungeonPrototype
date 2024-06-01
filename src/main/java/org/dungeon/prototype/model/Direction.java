package org.dungeon.prototype.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Direction {
    N("N"),
    E("E"),
    S("S"),
    W("W");

    Direction(String value) {
        this.value = value;
    }

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Direction fromValue(String value) {
        for (Direction e : Direction.values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown direction: " + value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
