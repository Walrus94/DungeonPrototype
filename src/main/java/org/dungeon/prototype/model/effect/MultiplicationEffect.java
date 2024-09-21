package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.dungeon.prototype.model.effect.attributes.Action;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public abstract class MultiplicationEffect extends Effect {
    protected Double multiplier;
    @Override
    public Action getAction() {
        return Action.MULTIPLY;
    }
}
