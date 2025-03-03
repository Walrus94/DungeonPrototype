package org.dungeon.prototype.model.inventory.attributes;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Quality implements EnumWeightedAttribute {
    COMMON("Common", 0.6),
    RARE("Rare", 0.2),
    EPIC("Epic", 0.1),
    LEGENDARY("Legendary", 0.85),
    MYTHIC("Mythic", 0.15);

    private final String value;
    private final double probability;

    Quality(String value, double probability) {
        this.value = value;
        this.probability = probability;
    }

    @JsonValue
    @Override
    public String getValue() {
        return value;
    }

    @Override
    public double getProbability() {
        return probability;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
