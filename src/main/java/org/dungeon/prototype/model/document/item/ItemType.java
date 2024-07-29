package org.dungeon.prototype.model.document.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum ItemType implements EnumAttribute {
    WEAPON("Weapon"),
    WEARABLE("Wearable");

    private final String value;

    ItemType(String value) {
        this.value = value;
    }

    @Override
    @JsonProperty
    public String getValue() {
        return value;
    }
}
