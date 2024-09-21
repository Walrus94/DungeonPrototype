package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.val;
import org.dungeon.prototype.model.weight.Weight;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class PermanentMultiplicationEffect extends MultiplicationEffect {

    @Override
    public Weight getWeight() {
        return Weight.buildWeightVectorForAttribute(attribute, 1.0 - multiplier);
    }

    @Override
    public Boolean isPermanent() {
        return true;
    }

    @Override
    public String toString() {
        val twoDigits = (int) ((multiplier * 100) % 100);
        if (multiplier < 1) {
            return String.format("Decreases %s by %d%%", attribute, 100 - twoDigits);
        } else {
            return String.format("Increases %s by %d%%", attribute, twoDigits);
        }
    }
}
