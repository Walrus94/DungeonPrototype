package org.dungeon.prototype.service.effect;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.effect.AdditionEffect;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.ExpirableEffect;
import org.dungeon.prototype.model.effect.MultiplicationEffect;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttack;
import org.dungeon.prototype.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.math3.util.FastMath.max;
import static org.apache.commons.math3.util.FastMath.min;
import static org.dungeon.prototype.model.effect.attributes.Action.ADD;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.MAX_ARMOR;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.HEALTH;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.HEALTH_MAX;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.HEALTH_MAX_ONLY;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.MANA;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.MANA_MAX;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.MANA_MAX_ONLY;
import static org.dungeon.prototype.util.PlayerUtil.getPrimaryAttack;
import static org.dungeon.prototype.util.PlayerUtil.getSecondaryAttack;

@Slf4j
@Service
public class EffectService {
    @Autowired
    private PlayerService playerService;

    /**
     * Applies effects to monster, and removes expired ones
     * @param monster to apply changes to
     * @return updated monster
     */
    public Monster updateMonsterEffects(Monster monster) {
        val monsterAttack = monster.getCurrentAttack();
        monster.getEffects()
                .forEach(monsterEffect -> {
                    switch (monsterEffect.getAttribute()) {
                        case ATTACK -> monsterAttack.setAttack(monsterAttack.getAttack() + monsterEffect.getAmount());
                        case HEALTH -> monster.setHp(max(0, min(monster.getMaxHp(), monster.getHp() + monsterEffect.getAmount())));
                    }
                });
        removeExpiredEffects(monster.getEffects());
        return monster;
    }

    /**
     * Calculates effects imply on player attributes,
     * removes expired effects
     * @param player to apply changes to
     * @return updated player
     */
    public Player updatePlayerEffects(Player player) {
        Map<EffectAttribute, PriorityQueue<Effect>> mappedEffects = player.getEffects().stream()
                .collect(Collectors.groupingBy(Effect::getAttribute,
                        Collectors.toCollection(() -> new PriorityQueue<>(Comparator.comparing(Effect::getAction)))
                ));
        calculateHealthEffects(player, mappedEffects);
        calculateManaEffects(player, mappedEffects);
        calculateBattleEffects(player, mappedEffects);
        removeExpiredEffects(player.getEffects());
        return player;
    }

    /**
     * Calculates player armor effect
     * @param player to apply changes to
     * @return updated player
     */
    public Player updateArmorEffect(Player player) {
        val effects = player.getEffects().stream().filter(effect -> MAX_ARMOR.equals(effect.getAttribute()))
                .collect(Collectors.toCollection(() ->
                        new PriorityQueue<>(Comparator.comparing(Effect::getAction))));
        val maxDefense = summarizeEffects(effects, player.getInventory().calculateMaxDefense());
        player.setMaxDefense(maxDefense);
        if (isNull(player.getDefense()) || player.getDefense() > maxDefense) {
            player.setDefense(maxDefense);
        }
        return player;
    }

    private void calculateBattleEffects(Player player, Map<EffectAttribute, PriorityQueue<Effect>> mappedEffects) {
        val primaryWeapon = player.getInventory().getPrimaryWeapon();
        val secondaryWeapon = player.getInventory().getSecondaryWeapon();
        PlayerAttack primaryAttack = getPrimaryAttack(player, primaryWeapon);
        PlayerAttack secondaryAttack = getSecondaryAttack(player, secondaryWeapon);
        mappedEffects.forEach((attribute, playerEffects) -> {
            switch (attribute) {
                case ATTACK -> {
                    primaryAttack.setAttack(summarizeEffects(playerEffects, primaryWeapon.getAttack()));
                    if (nonNull(secondaryAttack)) {
                        secondaryAttack.setAttack(summarizeEffects(playerEffects, secondaryWeapon.getAttack()));
                    }
                }
                case CRITICAL_HIT_MULTIPLIER -> {
                    if (nonNull(primaryWeapon)) {
                        primaryAttack.setCriticalHitMultiplier(summarizeEffects(playerEffects, primaryWeapon.getCriticalHitMultiplier()));
                    }
                    if (nonNull(secondaryAttack)) {
                        secondaryAttack.setCriticalHitMultiplier(summarizeEffects(playerEffects, secondaryWeapon.getCriticalHitMultiplier()));
                    }
                }
                case CRITICAL_HIT_CHANCE -> {
                    primaryAttack.setCriticalHitChance(summarizeEffects(playerEffects, primaryWeapon.getCriticalHitChance()));
                    if (nonNull(secondaryAttack)) {
                        secondaryAttack.setCriticalHitChance(summarizeEffects(playerEffects, secondaryWeapon.getCriticalHitChance()));
                    }
                }
                case MISS_CHANCE -> {
                    primaryAttack.setChanceToMiss(summarizeEffects(playerEffects, primaryWeapon.getChanceToMiss()));
                    if (nonNull(secondaryAttack)) {
                        secondaryAttack.setChanceToMiss(summarizeEffects(playerEffects, secondaryWeapon.getChanceToMiss()));
                    }
                }
                case KNOCK_OUT_CHANCE -> {
                    primaryAttack.setChanceToKnockOut(summarizeEffects(playerEffects, primaryWeapon.getChanceToKnockOut()));
                    if (nonNull(secondaryAttack)) {
                        secondaryAttack.setChanceToKnockOut(summarizeEffects(playerEffects, secondaryWeapon.getChanceToKnockOut()));
                    }
                }
                case CHANCE_TO_DODGE -> {
                    if (nonNull(player.getInventory().getBoots())) {
                        Double value = summarizeEffects(playerEffects, player.getInventory().getBoots().getChanceToDodge());
                        player.setChanceToDodge(value);
                    }
                }
                case XP_BONUS -> player.setXpBonus(summarizeEffects(playerEffects, 1.0));
                case GOLD_BONUS -> player.setGoldBonus(summarizeEffects(playerEffects, 1.0));
            }
        });
        player.setPrimaryAttack(primaryAttack);
        player.setSecondaryAttack(secondaryAttack);
    }

    private void calculateHealthEffects(Player player, Map<EffectAttribute, PriorityQueue<Effect>> mappedEffects) {
        val defaultMaxHp = playerService.getDefaultMaxHp(player);
        val MaxHpEffects = getEffectsPriorityQueue(mappedEffects,
                (attribute -> Set.of(HEALTH_MAX, HEALTH_MAX_ONLY).contains(attribute)));
        val maxHp = summarizeEffects(MaxHpEffects, defaultMaxHp);
        val combinedHpEffects = mappedEffects.entrySet().stream()
                .filter(entry -> HEALTH_MAX.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(() ->
                        new PriorityQueue<>(Comparator.comparing(Effect::getAction))));
        final ArrayList<AdditionEffect> hpOnlyEffects = getBarEffectsList(mappedEffects, HEALTH);
        var hp = summarizeAdditionalBarEffects(combinedHpEffects, player.getHp());
        hp = summarizeBarEffect(hpOnlyEffects, hp);
        player.setMaxHp(maxHp);
        player.setHp(max(0, min(maxHp, hp)));
    }

    private void calculateManaEffects(Player player, Map<EffectAttribute, PriorityQueue<Effect>> mappedEffects) {
        val defaultMaxMana = playerService.getDefaultMaxMana(player);
        val MaxManaEffects = getEffectsPriorityQueue(mappedEffects,
                attribute -> Set.of(MANA_MAX, MANA_MAX_ONLY).contains(attribute));
        val maxMana = summarizeEffects(MaxManaEffects, defaultMaxMana);
        val combinedManaEffects = mappedEffects.entrySet().stream()
                .filter(entry -> MANA_MAX.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(() ->
                        new PriorityQueue<>(Comparator.comparing(Effect::getAction))));
        final ArrayList<AdditionEffect> manaOnlyEffects = getBarEffectsList(mappedEffects, MANA);
        var mana = summarizeAdditionalBarEffects(combinedManaEffects, player.getMana());
        mana = summarizeBarEffect(manaOnlyEffects, mana);
        player.setMaxMana(maxMana);
        player.setMana(max(0, min(maxMana, mana)));
    }

    private static PriorityQueue<Effect> getEffectsPriorityQueue(Map<EffectAttribute, PriorityQueue<Effect>> mappedEffects,
                                                                 Predicate<EffectAttribute> attributePredicate) {
        return mappedEffects.entrySet().stream()
                .filter(entry -> attributePredicate.test(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(() ->
                        new PriorityQueue<>(Comparator.comparing(Effect::getAction))));
    }

    private static ArrayList<AdditionEffect> getBarEffectsList(Map<EffectAttribute, PriorityQueue<Effect>> mappedEffects,
                                                                EffectAttribute bar) {
        return mappedEffects.entrySet().stream()
                .filter(entry -> bar.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .filter(effect -> effect instanceof AdditionEffect)
                .map(effect -> (AdditionEffect) effect)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Double summarizeEffects(PriorityQueue<Effect> effects, Double initialValue) {
        val effectsMap = effects.stream()
                .collect(Collectors.groupingBy(Effect::getAction));
        return effectsMap.values().stream().mapToDouble(playerEffects -> playerEffects.stream()
                        .filter(e -> e instanceof MultiplicationEffect)
                        .map(effect -> ((MultiplicationEffect)effect).getMultiplier())
                        .reduce(initialValue, (m1, m2) -> m1 * m2))
                .findFirst().orElse(initialValue);
    }

    private Integer summarizeEffects(PriorityQueue<Effect> effects, Integer initialValue) {
        val effectsMap = effects.stream()
                .collect(Collectors.groupingBy(Effect::getAction));
        return effectsMap.values().stream().mapToInt(playerEffects -> {
            Double multiplyFactor = playerEffects.stream()
                    .filter(effect -> effect instanceof MultiplicationEffect)
                    .map(effect -> ((MultiplicationEffect) effect).getMultiplier())
                    .reduce(initialValue.doubleValue(), (m1, m2) -> m1 * m2);
            return playerEffects.stream()
                    .filter(e -> e instanceof AdditionEffect)
                    .map(effect -> ((AdditionEffect) effect).getAmount())
                    .reduce(multiplyFactor.intValue(), Integer::sum);
        }).findFirst().orElse(initialValue);
    }

    private Integer summarizeAdditionalBarEffects(PriorityQueue<Effect> effects, Integer initialValue) {
        val effectsMap = effects.stream()
                .collect(Collectors.groupingBy(Effect::getAction));
        return effectsMap.values().stream().mapToInt(playerEffects -> {
            Double multiplyFactor = playerEffects.stream()
                    .filter(effect -> effect instanceof MultiplicationEffect)
                    .filter(effect -> !effect.hasFirstTurnPassed())
                    .map(effect -> ((MultiplicationEffect) effect).getMultiplier())
                    .reduce(initialValue.doubleValue(), (m1, m2) -> m1 * m2);
            return playerEffects.stream()
                    .filter(e -> e instanceof AdditionEffect)
                    .filter(effect -> !effect.hasFirstTurnPassed())
                    .map(effect -> ((AdditionEffect) effect).getAmount())
                    .reduce(multiplyFactor.intValue(), Integer::sum);
        }).findFirst().orElse(initialValue);
    }

    private Integer summarizeBarEffect(List<AdditionEffect> effects, Integer initialValue) {
        val effectsMap = effects.stream()
                .collect(Collectors.groupingBy(Effect::getAction));
        return effectsMap.values().stream().mapToInt(playerEffects -> playerEffects.stream()
                .filter(e -> ADD.equals(e.getAction()))
                .map(AdditionEffect::getAmount)
                .reduce(initialValue, Integer::sum)).findFirst().orElse(initialValue);
    }

    private <T extends Effect> void removeExpiredEffects(List<T> effects) {
        val effectsToRemove = effects.stream()
                .filter(effect -> effect instanceof ExpirableEffect)
                .filter(effect -> ((ExpirableEffect) effect).decreaseTurnsLeft() < 1)
                .toList();
        effects.removeAll(effectsToRemove);
    }
}
