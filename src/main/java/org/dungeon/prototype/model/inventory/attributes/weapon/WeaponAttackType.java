package org.dungeon.prototype.model.inventory.attributes.weapon;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum WeaponAttackType implements EnumAttribute {
    STAB("Stab"),
    SLASH("Slash"),
    BLUNT("Blunt"),
    STRIKE("Strike");

    private final String value;

    WeaponAttackType(String value) {
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
    @JsonCreator
    public static WeaponAttackType fromValue(String value) {
        for (WeaponAttackType e : WeaponAttackType.values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown attack type: " + value);
    }
}
