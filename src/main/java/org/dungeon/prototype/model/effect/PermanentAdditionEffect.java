package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.dungeon.prototype.model.weight.Weight;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class PermanentAdditionEffect extends AdditionEffect {
    @Override
    public Boolean isPermanent() {
        return true;
    }

    @Override
    public Weight getWeight() {
        return Weight.buildWeightVectorForAttribute(attribute, amount);
    }

    @Override
    public String toString() {
        if (amount < 0) {
            return String.format("Subtracts %d from %s", -amount, attribute);
        } else {
            return String.format("Adds %d to %s", amount, attribute);
        }
    }
}
