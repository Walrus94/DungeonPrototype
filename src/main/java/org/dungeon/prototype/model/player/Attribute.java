package org.dungeon.prototype.model.player;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Attribute {
    POWER("Power"), //Affects attack
    STAMINA("Stamina"), //Affects health
    RECEPTION("Reception"), //Affects time of effects (both positive and negative)
    MAGIC("Magic"), //Affects amount of mana and damage from magic weapons
    LUCK("Luck"); //Affects critical hits strike, rare items discover (e.g. any random events)

    Attribute(String value) {
        this.value = value;
    }

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Attribute fromValue(String value) {
        for (Attribute e : Attribute.values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown attribute: " + value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
