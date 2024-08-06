package org.dungeon.prototype.model.inventory.attributes;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Quality implements EnumAttribute {
    COMMON("Common"),
    RARE("Rare"),
    EPIC("Epic"),
    LEGENDARY("Legendary"),
    MYTHIC("Mythic");

    private final String value;

    Quality(String value) {
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
