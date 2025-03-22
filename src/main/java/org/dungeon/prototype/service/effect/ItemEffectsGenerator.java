package org.dungeon.prototype.service.effect;

import lombok.val;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.async.AsyncJobHandler;
import org.dungeon.prototype.exception.DungeonPrototypeException;
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
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
    private AsyncJobHandler asyncJobHandler;
    @Autowired
    private ItemsGenerationProperties itemsGenerationProperties;

    /**
     * Generates configured amount of effects to change item's weight norm by given delta
     * @param chatId current chat's id
     * @param itemId id of item to apply effect to
     * @param expectedWeightChange expected weight norm delta
     * @return added effect's weight norm
     */
    public Optional<Double> addItemEffect(Long chatId, String itemId, double expectedWeightChange) {
        try {
            return (Optional<Double>) asyncJobHandler.submitItemUpdateTask(() -> {
                val item = itemService.findItem(chatId, itemId);
                if (nonNull(item)) {
                    if (item instanceof Usable) {
                        //TODO: fix after implementing usable
                        return Optional.empty();
                    }
                    val minEffectsAmount = itemsGenerationProperties.getEffects().getMinimumAmountPerItemMap().get(item.getAttributes().getQuality());
                    val maxEffectsAmount = itemsGenerationProperties.getEffects().getMaximumAmountPerItemMap().get(item.getAttributes().getQuality());

                    if (item.getEffects().size() < minEffectsAmount) {
                        val amount = minEffectsAmount - item.getEffects().size();
                        double sum = 0.0;
                        for (int i = 0; i < amount; i++) {
                            sum += generateAndAddItemEffect(expectedWeightChange / amount, item).orElse(0.0);
                        }
                        return Optional.of(sum);
                    } else if (item.getEffects().size() < maxEffectsAmount) {
                        return generateAndAddItemEffect(expectedWeightChange, item);
                    }
                }
                return Optional.empty();
            }, chatId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DungeonPrototypeException(e.getMessage());
        }
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
        try {
            return (Pair<String, Double>) asyncJobHandler.submitItemUpdateTask(() -> {
                val vanillaItem = itemService.findItem(chatId, itemId);
                if (nonNull(vanillaItem)) {
                    switch (vanillaItem.getItemType()) {
                        case WEAPON -> {
                            val weapon = new Weapon((Weapon) vanillaItem);
                            if (generateAndAddItemEffect(expectedWeightChange, weapon).isPresent()) {
                                return Pair.create(weapon.getId(), weapon.getWeight().toVector().getNorm());
                            }
                        }
                        case WEARABLE -> {
                            val wearable = new Wearable((Wearable) vanillaItem);
                            if (generateAndAddItemEffect(expectedWeightChange, wearable).isPresent()) {
                                return Pair.create(wearable.getId(), wearable.getWeight().toVector().getNorm());
                            }
                        }
                        case USABLE -> {
                            val usable = new Usable((Usable) vanillaItem);
                            if (generateAndAddItemEffect(expectedWeightChange, usable).isPresent()) {
                                return Pair.create(usable.getId(), usable.getWeight().toVector().getNorm());
                            }
                        }
                    }
                }
                return null;
            }, chatId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DungeonPrototypeException(e.getMessage());
        }
    }

    private Optional<Double> generateAndAddItemEffect(double expectedWeightChange, Item item) {
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
            //TODO: add case for usable
            return Optional.empty();
        }
        effect = effectFactory.generateItemEffect(item, attribute, action, expectedWeightChange);
        item.getEffects().add(effect);
        item = itemService.saveItem(item);
        return Optional.of(item.getWeight().toVector().getNorm());
    }
}
