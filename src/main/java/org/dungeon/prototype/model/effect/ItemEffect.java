package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;

import static org.dungeon.prototype.model.effect.EffectApplicant.ITEM;

@Data
@EqualsAndHashCode(callSuper = false)
public class ItemEffect extends PlayerEffect {
    @Override
    public EffectApplicant getApplicableTo() {
        return ITEM;
    }

    @Override
    public Boolean isPermanent() {
        return true;
    }
}
