package org.dungeon.prototype.model.monster;

import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;

public enum MonsterClass implements EnumAttribute {
    WEREWOLF("Werewolf"),
    SWAMP_BEAST("Swamp beast"),
    VAMPIRE("Vampire"),
    DRAGON("Dragon"),
    ZOMBIE("Zombie");

    private final String value;

    MonsterClass(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
