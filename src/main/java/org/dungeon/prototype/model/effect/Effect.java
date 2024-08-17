package org.dungeon.prototype.model.effect;

import lombok.Data;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.annotations.validation.MultiConditionalNotNull;

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


    protected Boolean hasFirstTurnPassed = false;

    public abstract EffectAttribute getAttribute();
    public abstract Boolean isPermanent();
    public abstract Boolean isNegative();

    public boolean isApplicable() {
        if (!getHasFirstTurnPassed()) {
            return true;
        }
        return !isPermanent() && ((Expirable) this).getIsAccumulated();
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
