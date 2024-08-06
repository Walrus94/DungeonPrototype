package org.dungeon.prototype.service.effect;

import lombok.val;
import org.dungeon.prototype.model.effect.ItemEffect;
import org.dungeon.prototype.model.effect.attributes.PlayerEffectAttribute;
import org.dungeon.prototype.model.effect.Action;
import org.dungeon.prototype.properties.ItemsGenerationProperties;
import org.dungeon.prototype.repository.EffectRepository;
import org.dungeon.prototype.repository.converters.mapstruct.EffectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dungeon.prototype.model.effect.attributes.PlayerEffectAttribute.*;
import static org.dungeon.prototype.util.RandomUtil.*;

@Component
public class ItemEffectsGenerator {
    @Autowired
    ItemsGenerationProperties itemsGenerationProperties;
    @Autowired
    EffectRepository effectRepository;
    @Value("${generation.items.effects.minimum-amount-per-item}")
    private Integer minEffectsAmount;
    @Value("${generation.items.effects.maximum-amount-per-item}")
    private Integer maxEffectsAmount;

    public List<ItemEffect> generateEffects() {
        val properties = itemsGenerationProperties.getEffects();
        val amount = getRandomInt(minEffectsAmount, maxEffectsAmount);
        return Stream.generate(() -> {
            val effect = new ItemEffect();
            val attribute = PlayerEffectAttribute.values()[getRandomInt(0, PlayerEffectAttribute.values().length - 1)];
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
            val savedEffect = effectRepository.save(EffectMapper.INSTANCE.mapToDocument(effect));
            return EffectMapper.INSTANCE.mapToItemEffect(savedEffect);
        }).limit(amount).collect(Collectors.toList());
    }
}
