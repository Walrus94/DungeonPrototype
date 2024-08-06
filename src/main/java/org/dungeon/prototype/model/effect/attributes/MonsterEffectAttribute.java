package org.dungeon.prototype.model.effect.attributes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MonsterEffectAttribute implements EffectAttribute {
    ATTACK("Attack"),
    HEALTH("Health"),
    MOVING("Moving");
    private final String value;

    MonsterEffectAttribute(String value) {
        this.value = value;
    }

    @JsonValue
    @Override
    public String getValue() {
        return value;
    }


    @JsonCreator
    public static MonsterEffectAttribute fromValue(String value) {
        for (MonsterEffectAttribute e : MonsterEffectAttribute.values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown monster effect attribute type: " + value);
    }
    @Override
    public String toString() {
        return this.value;
    }
}
