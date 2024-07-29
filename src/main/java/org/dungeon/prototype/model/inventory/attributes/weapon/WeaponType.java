package org.dungeon.prototype.model.inventory.attributes.weapon;

import com.fasterxml.jackson.annotation.JsonValue;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum WeaponType implements EnumAttribute {
    SWORD("Sword"),
    AXE("Axe"),
    DAGGER("Dagger"),
    CLUB("Club"),
    MACE("Mace"),
    STAFF("Staff"),
    SPEAR("Spear");

    private final String value;

    WeaponType(String value) {
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
