package org.dungeon.prototype.model.effect;

import lombok.Data;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.validation.MultiConditionalNotNull;

@Data
@MultiConditionalNotNull(conditions = {
        @MultiConditionalNotNull.Condition(
                field = "multiplier",
                conditionalField = "action",
                conditionalValues = {"MULTIPLY"}
        ),
        @MultiConditionalNotNull.Condition(
                field = "amount",
                conditionalField = "action",
                conditionalValues = {"ADD"}
        )
})
public abstract class Effect {
    protected String id;
    protected EffectApplicant applicableTo;

    protected Action action;
    protected Integer amount;
    protected Double multiplier;

    protected Integer weight;

    public abstract EffectAttribute getAttribute();
    abstract Boolean isPermanent();
    abstract Boolean isNegative();
}
