package org.dungeon.prototype.model.effect;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.effect.attributes.MonsterEffectAttribute;
import org.dungeon.prototype.util.GenerationUtil;

import static java.util.Objects.isNull;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class MonsterEffect extends Effect implements Expirable {
    MonsterEffectAttribute attribute;
    private Integer turnsLasts;
    private Boolean isAccumulated;
    @Override
    public Boolean isPermanent() {
        return isNull(turnsLasts) || !(turnsLasts > 0);
    }

    @Override
    public Boolean isNegative() {
        return GenerationUtil.isNegative(action, amount, multiplier);
    }

    @Override
    public Integer decreaseTurnsLasts() {
        return turnsLasts--;
    }

}
