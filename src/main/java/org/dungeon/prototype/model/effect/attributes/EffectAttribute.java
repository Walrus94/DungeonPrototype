package org.dungeon.prototype.model.effect.attributes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum EffectAttribute implements EnumAttribute {
    HEALTH("Health"),
    HEALTH_MAX("Maximum health"),
    HEALTH_MAX_ONLY("Maximum Health only"),
    MANA("Mana"),
    MANA_MAX("Maximum mana"),
    MANA_MAX_ONLY("Maximum Mana only"),
    ATTACK("Attack"),
    ARMOR("Armor"),
    CRITICAL_HIT_MULTIPLIER("Critical hit multiplier"),
    CRITICAL_HIT_CHANCE("Critical hit chance"),
    MISS_CHANCE("Miss chance"),
    KNOCK_OUT_CHANCE("Knock out chance"),
    CHANCE_TO_DODGE("Chance to dodge"),
    MOVING("Moving");

    private final String value;

    EffectAttribute(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static EffectAttribute fromValue(String value) {
        for (EffectAttribute e : EffectAttribute.values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown player effect attribute type: " + value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
