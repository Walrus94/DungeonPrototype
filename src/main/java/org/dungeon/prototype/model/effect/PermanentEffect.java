package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PermanentEffect extends Effect {
    @Override
    public Boolean isPermanent() {
        return true;
    }

    @Override
    public String toString() {
        switch (action) {
            case ADD -> {
                return "Adds " + amount + " to " + getAttribute().toString();
            }
            case MULTIPLY -> {
                return "Multiplies " + getAttribute().toString() + " by " + multiplier;
            }
        }
        return super.toString();
    }
}
