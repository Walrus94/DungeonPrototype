package org.dungeon.prototype.model.monster;

import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum MonsterAttackType implements EnumAttribute {
    SLASH("Slash"),
    GROWL("Growl"),
    BITE("Bite"),
    VAMPIRE_BITE("Vampire bite"),
    POISON_SPIT("Poison spit"),
    FIRE_SPIT("Fire spit");

    private final String value;

    MonsterAttackType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
