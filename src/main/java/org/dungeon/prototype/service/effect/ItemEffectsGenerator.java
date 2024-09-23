package org.dungeon.prototype.service.effect;

import lombok.val;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.attributes.Action;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.items.Usable;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.properties.ItemsGenerationProperties;
import org.dungeon.prototype.service.item.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Objects.nonNull;
import static org.dungeon.prototype.model.effect.attributes.Action.MULTIPLY;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.*;
import static org.dungeon.prototype.util.RandomUtil.getRandomInt;

@Component
public class ItemEffectsGenerator {
    @Autowired
    private ItemService itemService;
    @Autowired
    private EffectFactory effectFactory;
    @Autowired
    private ItemsGenerationProperties itemsGenerationProperties;

    /**
     * Generates configured amount of effects to change item's weight norm by given delta
     * @param chatId current chat's id
     * @param itemId id of item to apply effect to
     * @param expectedWeightChange expected weight norm delta
     * @return added effect's weight norm
     */
    public double addItemEffect(Long chatId, String itemId, double expectedWeightChange) {
        val item = itemService.findItem(chatId, itemId);
        if (nonNull(item)) {
            if (item instanceof Usable) {
                return 0.0;
            }
            val minEffectsAmount = itemsGenerationProperties.getEffects().getMinimumAmountPerItemMap().get(item.getAttributes().getQuality());
            val maxEffectsAmount = itemsGenerationProperties.getEffects().getMaximumAmountPerItemMap().get(item.getAttributes().getQuality());

            if (item.getEffects().size() < minEffectsAmount) {
                val amount = minEffectsAmount - item.getEffects().size();
                double sum = 0.0;
                for (int i = 0; i < amount; i++) {
                     sum += generateAndAddItemEffect(expectedWeightChange / amount, item);
                }
                return sum;
            } else if (item.getEffects().size() < maxEffectsAmount) {
                return generateAndAddItemEffect(expectedWeightChange, item);
            }
        }
        return 0.0;
    }

    /**
     * Generates configured amount of effects to change item's weight norm by given delta
     * and adds to copy of initial item
     * @param chatId current chat's id
     * @param itemId id of item to copy and apply effect to
     * @param expectedWeightChange expected weight norm delta
     * @return id and weight norm of newly created item
     */
    public Pair<String, Double> copyItemAndAddEffect(Long chatId, String itemId, double expectedWeightChange) {
        val vanillaItem = itemService.findItem(chatId, itemId);
        if (nonNull(vanillaItem)) {
            switch (vanillaItem.getItemType()) {
                case WEAPON -> {
                    val weapon = new Weapon((Weapon) vanillaItem);
                    if (generateAndAddItemEffect(expectedWeightChange, weapon) > 0.0) {
                        return Pair.create(weapon.getId(), weapon.getWeight().toVector().getNorm());
                    }
                }
                case WEARABLE -> {
                    val wearable = new Wearable((Wearable) vanillaItem);
                    if (generateAndAddItemEffect(expectedWeightChange, wearable) > 0.0) {
                        return Pair.create(wearable.getId(), wearable.getWeight().toVector().getNorm());
                    }
                }
                case USABLE -> {
                    val usable = new Usable((Usable) vanillaItem);
                    if (generateAndAddItemEffect(expectedWeightChange, usable) > 0.0) {
                        return Pair.create(usable.getId(), usable.getWeight().toVector().getNorm());
                    }
                }
            }
        }
        return null;
    }

    private double generateAndAddItemEffect(double expectedWeightChange, Item item) {
        Effect effect;
        Action action;
        EffectAttribute attribute;
        List<EffectAttribute> applicableAttributes;
        if (item instanceof Wearable wearable) {
            applicableAttributes = switch (wearable.getAttributes().getWearableType()) {
                case HELMET -> List.of(CHANCE_TO_DODGE, XP_BONUS, GOLD_BONUS);
                case VEST -> List.of(CRITICAL_HIT_MULTIPLIER, CHANCE_TO_DODGE, MISS_CHANCE, XP_BONUS, GOLD_BONUS);
                case GLOVES ->
                        List.of(CRITICAL_HIT_CHANCE, MISS_CHANCE, KNOCK_OUT_CHANCE, CRITICAL_HIT_MULTIPLIER, XP_BONUS, GOLD_BONUS);
                case BOOTS -> List.of(CRITICAL_HIT_CHANCE, KNOCK_OUT_CHANCE, MISS_CHANCE, XP_BONUS, GOLD_BONUS);
            };
            attribute = applicableAttributes.get(getRandomInt(0, applicableAttributes.size() - 1));
            action = MULTIPLY;
        } else if (item instanceof Weapon) {
            applicableAttributes = List.of(CHANCE_TO_DODGE, XP_BONUS, GOLD_BONUS, MAX_ARMOR,
                            HEALTH_MAX, HEALTH_MAX_ONLY, MANA_MAX, MANA_MAX_ONLY);
            attribute = applicableAttributes.get(getRandomInt(0, applicableAttributes.size() - 1));
            if (attribute.equals(CHANCE_TO_DODGE)) {
                action = MULTIPLY;
            } else {
                action = Action.values()[getRandomInt(0, Action.values().length)];
            }
        } else {
            return 0.0;
        }
        effect = effectFactory.generateItemEffect(item, attribute, action, expectedWeightChange);
        item.getEffects().add(effect);
        item = itemService.saveItem(item);
        return item.getWeight().toVector().getNorm();
    }
}
