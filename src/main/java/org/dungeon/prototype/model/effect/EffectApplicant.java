package org.dungeon.prototype.model.effect;

import com.fasterxml.jackson.annotation.JsonValue;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum EffectApplicant implements EnumAttribute {
    MONSTER("Monster"),
    PLAYER("Player"),
    ITEM("Item");

    private final String value;

    EffectApplicant(String value) {
        this.value = value;
    }

    @JsonValue
    @Override
    public String getValue() {
        return value;
    }
}
