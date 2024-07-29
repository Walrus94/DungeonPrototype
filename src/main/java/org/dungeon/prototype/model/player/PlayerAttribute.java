package org.dungeon.prototype.model.player;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum PlayerAttribute implements EnumAttribute {
    POWER("Power"), //Affects attack
    STAMINA("Stamina"), //Affects health
    PERCEPTION("Perception"), //Affects time of effects (both positive and negative) //TODO: should affect weapon specs increase
    MAGIC("Magic"), //Affects amount of mana and damage from magic weapons
    LUCK("Luck"); //Affects critical hits strike, rare items discover (e.g. any random events)

    PlayerAttribute(String value) {
        this.value = value;
    }

    private final String value;

    @JsonValue
    @Override
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PlayerAttribute fromValue(String value) {
        for (PlayerAttribute e : PlayerAttribute.values()) {
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
