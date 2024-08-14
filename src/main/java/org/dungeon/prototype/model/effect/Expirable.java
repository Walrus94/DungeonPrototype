package org.dungeon.prototype.model.effect;

public interface Expirable {
    Integer getTurnsLasts();
    Boolean getIsAccumulated();
    Boolean getHasFirstTurnPassed();

    Integer decreaseTurnsLasts();
}
