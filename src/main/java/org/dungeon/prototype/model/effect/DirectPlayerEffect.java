package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;

import static java.util.Objects.isNull;
import static org.dungeon.prototype.model.effect.EffectApplicant.PLAYER;

@Data
@EqualsAndHashCode(callSuper = false)
public class DirectPlayerEffect extends PlayerEffect {
    private Integer turnsLasts;
    @Override
    public EffectApplicant getApplicableTo() {
        return PLAYER;
    }

    @Override
    Boolean isPermanent() {
        return isNull(turnsLasts) || !(turnsLasts > 0);
    }
}
