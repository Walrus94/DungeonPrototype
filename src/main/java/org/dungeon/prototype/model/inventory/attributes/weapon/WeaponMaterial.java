package org.dungeon.prototype.model.inventory.attributes.weapon;

import com.fasterxml.jackson.annotation.JsonValue;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum WeaponMaterial implements EnumAttribute {
    WOOD("Wood"),
    STONE("Stone"),
    IRON("Iron"),
    STEEL("Steel"),
    PLATINUM("Platinum"),
    DIAMOND("Diamond"),
    MITHRIL("Mithril"),
    OBSIDIAN("Obsidian"),
    DRAGON_BONE("Dragon bone"),
    ENCHANTED_WOOD("Enchanted wood");

    private final String value;

    WeaponMaterial(String value) {
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
