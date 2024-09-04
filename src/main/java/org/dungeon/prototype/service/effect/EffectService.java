package org.dungeon.prototype.service.effect;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.ExpirableEffect;
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
import static org.dungeon.prototype.model.effect.attributes.Action.MULTIPLY;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.ARMOR;
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

    public Monster updateMonsterEffects(Monster monster) {
        removeExpiredEffects(monster.getEffects());
        val monsterAttack = monster.getCurrentAttack();
        monster.getEffects().stream()
                .forEach(monsterEffect -> {
                    switch (monsterEffect.getAttribute()) {
                        case ATTACK -> {
                            switch (monsterEffect.getAction()) {
                                case ADD ->
                                        monsterAttack.setAttack(monsterAttack.getAttack() + monsterEffect.getAmount());
                                case MULTIPLY ->
                                        monsterAttack.setAttack((int) (monsterAttack.getAttack() * monsterEffect.getMultiplier()));
                            }
                        }
                        case HEALTH -> {
                            switch (monsterEffect.getAction()) {
                                case ADD ->
                                        monster.setHp(min(monster.getMaxHp(), monster.getHp() + monsterEffect.getAmount()));
                                case MULTIPLY ->
                                        monster.setHp((int) min(monster.getMaxHp(), monster.getHp() * monsterEffect.getMultiplier()));
                            }
                        }
                    }
                    monsterEffect.decreaseTurnsLasts();
                });
        return monster;
    }

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
            }
        });
        player.setPrimaryAttack(primaryAttack);
        player.setSecondaryAttack(secondaryAttack);
    }

    private void calculateHealthEffects(Player player, Map<EffectAttribute, PriorityQueue<Effect>> mappedEffects) {
        val defaultMaxHp = playerService.getDefaultMaxHp(player);
        val baseHp = player.getHp() * defaultMaxHp / player.getMaxHp() ;
        val MaxHpEffects = getEffectsPriorityQueue(mappedEffects,
                (attribute -> Set.of(HEALTH_MAX, HEALTH_MAX_ONLY).contains(attribute)));
        val maxHp = summarizeEffects(MaxHpEffects, defaultMaxHp);
        val hpEffects = mappedEffects.entrySet().stream()
                .filter(entry -> HEALTH_MAX.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(() ->
                        new PriorityQueue<>(Comparator.comparing(Effect::getAction))));
        final ArrayList<ExpirableEffect> hpOnlyEffects = getBarEffectsList(mappedEffects, HEALTH);
        var hp = summarizeEffects(hpEffects, baseHp);
        hp = summarizeBarEffect(hpOnlyEffects, hp);
        player.setMaxHp(maxHp);
        player.setHp(max(0, min(maxHp, hp)));
    }

    private void calculateManaEffects(Player player, Map<EffectAttribute, PriorityQueue<Effect>> mappedEffects) {
        val defaultMaxMana = playerService.getDefaultMaxMana(player);
        val baseMana = player.getMana() * defaultMaxMana / player.getMaxMana() ;
        val MaxManaEffects = getEffectsPriorityQueue(mappedEffects,
                attribute -> Set.of(MANA_MAX, MANA_MAX_ONLY).contains(attribute));
        val maxMana = summarizeEffects(MaxManaEffects, defaultMaxMana);
        val manaEffects = mappedEffects.entrySet().stream()
                .filter(entry -> MANA_MAX.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(() ->
                        new PriorityQueue<>(Comparator.comparing(Effect::getAction))));
        final ArrayList<ExpirableEffect> manaOnlyEffects = getBarEffectsList(mappedEffects, MANA);
        var mana = summarizeEffects(manaEffects, baseMana);
        mana = summarizeBarEffect(manaOnlyEffects, mana);
        player.setMaxMana(maxMana);
        player.setMana(max(0, min(maxMana, mana)));
    }

    public Player updateArmorEffect(Player player) {
        val effects = player.getEffects().stream().filter(effect -> ARMOR.equals(effect.getAttribute()))
                .collect(Collectors.toCollection(() ->
                        new PriorityQueue<>(Comparator.comparing(Effect::getAction))));
        val maxDefense = summarizeEffects(effects, player.getInventory().calculateMaxDefense());
        player.setMaxDefense(maxDefense);
        if (isNull(player.getDefense()) || player.getDefense() > maxDefense) {
            player.setDefense(maxDefense);
        }
        return player;
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

    private static ArrayList<ExpirableEffect> getBarEffectsList(Map<EffectAttribute, PriorityQueue<Effect>> mappedEffects,
                                                                EffectAttribute bar) {
        return mappedEffects.entrySet().stream()
                .filter(entry -> bar.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .filter(effect -> effect instanceof ExpirableEffect)
                .map(effect -> (ExpirableEffect) effect)
                .filter(ExpirableEffect::getIsAccumulated)
                .filter(effect -> ADD.equals(effect.getAction()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Double summarizeEffects(PriorityQueue<Effect> effects, Double initialValue) {
        val effectsMap = effects.stream()
                .collect(Collectors.groupingBy(Effect::getAction));
        return effectsMap.values().stream().mapToDouble(playerEffects -> playerEffects.stream()
                        .filter(e -> MULTIPLY.equals(e.getAction()))
                        .map(Effect::getMultiplier)
                        .reduce(initialValue, (m1, m2) -> m1 * m2))
                .findFirst().orElse(initialValue);
    }

    private Integer summarizeEffects(PriorityQueue<Effect> effects, Integer initialValue) {
        val effectsMap = effects.stream()
                .collect(Collectors.groupingBy(Effect::getAction));
        return effectsMap.values().stream().mapToInt(playerEffects -> {
            Double multiplyFactor = playerEffects.stream()
                    .filter(e -> MULTIPLY.equals(e.getAction()))
                    .map(Effect::getMultiplier)
                    .reduce(initialValue.doubleValue(), (m1, m2) -> m1 * m2);
            return playerEffects.stream()
                    .filter(e -> ADD.equals(e.getAction()))
                    .map(Effect::getAmount)
                    .reduce(multiplyFactor.intValue(), Integer::sum);
        }).findFirst().orElse(initialValue);

    }

    private Integer summarizeBarEffect(List<ExpirableEffect> effects, Integer initialValue) {
        val effectsMap = effects.stream()
                .collect(Collectors.groupingBy(Effect::getAction));
        return effectsMap.values().stream().mapToInt(playerEffects -> playerEffects.stream()
                .filter(e -> ADD.equals(e.getAction()))
                .map(Effect::getAmount)
                .reduce(initialValue, Integer::sum)).findFirst().orElse(initialValue);
    }

    private <T extends Effect> void removeExpiredEffects(List<T> effects) {
        val effectsToRemove = effects.stream()
                .filter(effect -> effect instanceof ExpirableEffect)
                .filter(effect -> ((ExpirableEffect) effect).decreaseTurnsLasts() < 1)
                .toList();
        effects.removeAll(effectsToRemove);
    }
}
