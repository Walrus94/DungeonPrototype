package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.dungeon.prototype.model.effect.attributes.Action;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class AdditionEffect extends Effect {
    protected Integer amount;
    @Override
    public Action getAction() {
        return Action.ADD;
    }
}
