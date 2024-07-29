package org.dungeon.prototype.model.inventory.attributes.wearable;

import com.fasterxml.jackson.annotation.JsonValue;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum WearableMaterial implements EnumAttribute {
    CLOTH("Cloth"),
    LEATHER("Leather"),
    TREATED_LEATHER("Treated leather"),
    IRON("Iron"),
    STEEL("Steel"),
    CHAIN_MAIL("Chain-mail"),
    ENCHANTED_LEATHER("Enchanted leather"),
    MITHRIL("Mithril"),
    ELVEN_SILK("Elven silk"),
    WOOL("Wool");

    private final String value;

    WearableMaterial(String value) {
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
