package org.dungeon.prototype.model.effect;

public interface ExpirableEffect {

    Boolean isAccumulated();

    Boolean hasFirstTurnPassed();

    Integer decreaseTurnsLeft();

    Integer getTurnsLeft();
}
