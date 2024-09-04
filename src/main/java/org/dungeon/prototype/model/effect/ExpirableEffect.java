package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ExpirableEffect extends Effect {
    protected Integer turnsLasts;
    protected Boolean isAccumulated;
    protected Integer baseAmount = amount;

    @Override
    public Boolean isPermanent() {
        return false;
    }

    public Integer decreaseTurnsLasts() {
        if (isAccumulated) {
            amount += baseAmount;
        }
        return turnsLasts--;
    }

    @Override
    public String toString() {
        switch (action) {
            case ADD -> {
                return "Adds " + amount + " to " + getAttribute().toString() + " for " + turnsLasts + " turns";
            }
            case MULTIPLY -> {
                return "Multiplies " + getAttribute().toString() + " by " + multiplier + " for " + turnsLasts + " turns";
            }
        }
        return super.toString();
    }
}
