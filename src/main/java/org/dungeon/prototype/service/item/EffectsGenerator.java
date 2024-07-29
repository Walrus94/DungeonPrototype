package org.dungeon.prototype.service.item;

import lombok.val;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.effect.Attribute;
import org.dungeon.prototype.model.inventory.attributes.effect.Effect;
import org.dungeon.prototype.model.inventory.attributes.effect.Action;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dungeon.prototype.model.inventory.attributes.effect.Attribute.ARMOR;
import static org.dungeon.prototype.model.inventory.attributes.effect.Attribute.ATTACK;
import static org.dungeon.prototype.util.RandomUtil.*;

@Component
public class EffectsGenerator {
    private static final int MIN_EFFECTS_AMOUNT = 1;
    private static final int MAX_EFFECTS_AMOUNT = 3;
    public List<Effect> generateEffects(Class<? extends Item> itemClass) {
        val amount = getRandomInt(MIN_EFFECTS_AMOUNT, MAX_EFFECTS_AMOUNT);
        return Stream.generate(() -> {
            val effect = new Effect();
            val attribute = Attribute.values()[getRandomInt(0, Attribute.values().length - 1)];
            effect.setAttribute(attribute);
            val action = Set.of(ARMOR, ATTACK).contains(attribute) ? Action.ADD : Action.MULTIPLY;
            effect.setAction(action);
            //TODO: adjust weight calculation
            if (action.equals(Action.ADD)) {
                effect.setAmount(getRandomEffectAddition());
                effect.setWeight(effect.getAmount() * 10);
            } else {
                effect.setMultiplier(getRandomEffectMultiplier());
                effect.setWeight((int) (effect.getMultiplier() * 100));
            }
            effect.setIsPermanent(true);
            effect.setApplicableTo(itemClass);
            return effect;
        }).limit(amount).collect(Collectors.toList());
    }
}
