package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.dungeon.prototype.model.weight.Weight;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ExpirableAdditionEffect extends AdditionEffect implements ExpirableEffect {
    private Boolean isAccumulated;
    private Integer turnsLeft;
    @Override
    public Weight getWeight() {
        return Weight.buildWeightVectorForAttribute(attribute, amount * ((double) turnsLeft / (turnsLeft + 1)));
    }

    @Override
    public Boolean isPermanent() {
        return false;
    }

    @Override
    public Boolean isAccumulated() {
        return isAccumulated;
    }

    @Override
    public Integer decreaseTurnsLeft() {
        hasFirstTurnPassed = true;
        return --turnsLeft;
    }

    @Override
    public Integer getTurnsLeft() {
        return turnsLeft;
    }

    @Override
    public String toString() {
        String accumulated = isAccumulated ? " each turn" : "";
        if (amount < 0) {
            return String.format("Subtracts %d from %s%s for %d turns", -amount, attribute, accumulated, turnsLeft);
        } else {
            return String.format("Adds %d to %s%s for %d turns", amount, attribute, accumulated, turnsLeft);
        }
    }
}
