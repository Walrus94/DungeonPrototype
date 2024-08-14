package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;

import static java.util.Objects.isNull;
import static org.dungeon.prototype.model.effect.EffectApplicant.PLAYER;

@Data
@EqualsAndHashCode(callSuper = false)
public class DirectPlayerEffect extends PlayerEffect implements Expirable {
    private Integer turnsLasts;
    private Boolean isAccumulated;
    @Override
    public EffectApplicant getApplicableTo() {
        return PLAYER;
    }

    @Override
    public Boolean isPermanent() {
        return isNull(turnsLasts) || !(turnsLasts > 0);
    }

    @Override
    public Integer decreaseTurnsLasts() {
        return turnsLasts--;
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
