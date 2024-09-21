package org.dungeon.prototype.model.effect;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.dungeon.prototype.annotations.validation.MultiConditionalNotNull;
import org.dungeon.prototype.model.effect.attributes.Action;
import org.dungeon.prototype.model.effect.attributes.EffectApplicant;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.model.weight.Weight;

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
@SuperBuilder
@NoArgsConstructor
public abstract class Effect {
    protected String id;
    protected EffectApplicant applicableTo;
    protected Boolean isPermanent;
    protected EffectAttribute attribute;
    @Builder.Default
    protected Boolean hasFirstTurnPassed = false;

    public abstract Weight getWeight();
    public abstract Action getAction();

    public Boolean isPermanent() {
        return isPermanent;
    }

    public Boolean hasFirstTurnPassed() {
        return hasFirstTurnPassed;
    }
}
