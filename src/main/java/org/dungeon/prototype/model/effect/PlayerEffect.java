package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dungeon.prototype.model.effect.attributes.PlayerEffectAttribute;
import org.dungeon.prototype.util.GenerationUtil;

import static org.dungeon.prototype.model.effect.attributes.PlayerEffectAttribute.MISS_CHANCE;

@Data
@EqualsAndHashCode(callSuper = false)
public abstract class PlayerEffect extends Effect {
    protected PlayerEffectAttribute attribute;
    @Override
    public Boolean isNegative() {
        if (attribute.equals(MISS_CHANCE)){
            return !GenerationUtil.isNegative(action, amount, multiplier);
        } else {
            return GenerationUtil.isNegative(action, amount, multiplier);
        }
    }
}
