package org.dungeon.prototype.model.inventory.attributes.weapon;

import com.fasterxml.jackson.annotation.JsonValue;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum WeaponHandlerMaterial implements EnumAttribute {
    WOOD("Wood"),
    LEATHER("Leather"),
    STEEL("Steel"),
    TREATED_LEATHER("Treated leather"),
    DRAGON_BONE("Dragon bone");

    private final String value;

    WeaponHandlerMaterial(String value) {
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
