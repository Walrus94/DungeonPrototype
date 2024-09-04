package org.dungeon.prototype.service.effect;

import lombok.val;
import org.dungeon.prototype.model.effect.PermanentEffect;
import org.dungeon.prototype.model.effect.attributes.Action;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes;
import org.dungeon.prototype.properties.ItemsGenerationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.CHANCE_TO_DODGE;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.CRITICAL_HIT_CHANCE;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.KNOCK_OUT_CHANCE;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.MISS_CHANCE;
import static org.dungeon.prototype.util.RandomUtil.getRandomEffectAddition;
import static org.dungeon.prototype.util.RandomUtil.getRandomEffectMultiplier;
import static org.dungeon.prototype.util.RandomUtil.getRandomInt;

@Component
public class ItemEffectsGenerator {
    @Autowired
    ItemsGenerationProperties itemsGenerationProperties;

    public List<PermanentEffect> generateWeaponEffects(WeaponAttributes weaponAttributes) {
        val properties = itemsGenerationProperties.getEffects();
        val minEffectsAmount = itemsGenerationProperties.getEffects().getMinimumAmountPerItemMap().get(weaponAttributes.getQuality());
        val maxEffectsAmount = itemsGenerationProperties.getEffects().getMaximumAmountPerItemMap().get(weaponAttributes.getQuality());
        val amount = getRandomInt(minEffectsAmount, maxEffectsAmount);
        return Stream.generate(() -> {
            val effect = new PermanentEffect();
            val attribute = EffectAttribute.values()[getRandomInt(0, EffectAttribute.values().length - 1)];
            effect.setAttribute(attribute);
            val action = Set.of(CRITICAL_HIT_CHANCE, MISS_CHANCE, KNOCK_OUT_CHANCE, CHANCE_TO_DODGE).contains(attribute)
                    ? Action.MULTIPLY : Action.values()[getRandomInt(0, Action.values().length - 1)];
            effect.setAction(action);
            if (action.equals(Action.ADD)) {
                effect.setAmount(getRandomEffectAddition(properties.getRandomEffectAdditionMap()));
                effect.setWeight(effect.getAmount() * properties.getWeightAdditionRatio());
            } else {
                effect.setMultiplier(getRandomEffectMultiplier(properties.getRandomEffectMultiplierMap()));
                effect.setWeight((int) ((effect.getMultiplier() - 1.0) * properties.getWeightMultiplierRatio()));
            }
            return effect;
        }).limit(amount).collect(Collectors.toList());
    }

    public List<PermanentEffect> generateWearableEffects(WearableAttributes wearableAttributes) {
        val properties = itemsGenerationProperties.getEffects();
        val minEffectsAmount = itemsGenerationProperties.getEffects().getMinimumAmountPerItemMap().get(wearableAttributes.getQuality());
        val maxEffectsAmount = itemsGenerationProperties.getEffects().getMaximumAmountPerItemMap().get(wearableAttributes.getQuality());
        val amount = getRandomInt(minEffectsAmount, maxEffectsAmount);
        return Stream.generate(() -> {
            val effect = new PermanentEffect();
            val attribute = EffectAttribute.values()[getRandomInt(0, EffectAttribute.values().length - 1)];
            effect.setAttribute(attribute);
            val action = Set.of(CRITICAL_HIT_CHANCE, MISS_CHANCE, KNOCK_OUT_CHANCE, CHANCE_TO_DODGE).contains(attribute)
                    ? Action.MULTIPLY : Action.values()[getRandomInt(0, Action.values().length - 1)];
            effect.setAction(action);
            if (action.equals(Action.ADD)) {
                effect.setAmount(getRandomEffectAddition(properties.getRandomEffectAdditionMap()));
                effect.setWeight(effect.getAmount() * properties.getWeightAdditionRatio());
            } else {
                effect.setMultiplier(getRandomEffectMultiplier(properties.getRandomEffectMultiplierMap()));
                effect.setWeight((int) ((effect.getMultiplier() - 1.0) * properties.getWeightMultiplierRatio()));
            }
            return effect;
        }).limit(amount).collect(Collectors.toList());
    }
}
