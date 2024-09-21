package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.val;
import org.dungeon.prototype.model.weight.Weight;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class ExpirableMultiplicationEffect extends MultiplicationEffect implements ExpirableEffect {
    private Integer turnsLeft;
    @Override
    public Boolean isPermanent() {
        return false;
    }

    @Override
    public Boolean isAccumulated() {
        return false;
    }

    @Override
    public Integer decreaseTurnsLeft() {
        hasFirstTurnPassed = true;
        return --turnsLeft;
    }

    @Override
    public Weight getWeight() {
        return Weight.buildWeightVectorForAttribute(attribute, (1.0 - multiplier) * ((double) turnsLeft / (turnsLeft + 1)));
    }

    @Override
    public String toString() {
        val twoDigits = (int) ((multiplier * 100) % 100);
        String value;
        if (multiplier < 1) {
            value = String.format("Decreases %s by %d%%", attribute, 100 - twoDigits);
        } else {
            value = String.format("Increases %s by %d%%", attribute, twoDigits);
        }
        return value + String.format(" for %d turns", turnsLeft);
    }
}
