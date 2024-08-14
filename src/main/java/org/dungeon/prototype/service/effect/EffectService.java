package org.dungeon.prototype.service.effect;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.document.item.EffectDocument;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.Expirable;
import org.dungeon.prototype.model.effect.ItemEffect;
import org.dungeon.prototype.model.effect.PlayerEffect;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.repository.EffectRepository;
import org.dungeon.prototype.repository.converters.mapstruct.EffectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.math3.util.FastMath.max;
import static org.apache.commons.math3.util.FastMath.min;
import static org.dungeon.prototype.model.effect.Action.ADD;
import static org.dungeon.prototype.model.effect.Action.MULTIPLY;

@Slf4j
@Service
public class EffectService {
    @Autowired
    private EffectRepository effectRepository;

    public Monster updateMonsterEffects(Monster monster) {
        removeExpiredEffects(monster);
        val monsterAttack = monster.getCurrentAttack();
        monster.getEffects().stream()
                .filter(this::isApplicable)
                .forEach(monsterEffect -> {
                    switch (monsterEffect.getAttribute()) {
                        case MONSTER_ATTACK -> {
                            switch (monsterEffect.getAction()) {
                                case ADD ->
                                        monsterAttack.setAttack(monsterAttack.getAttack() + monsterEffect.getAmount());
                                case MULTIPLY ->
                                        monsterAttack.setAttack((int) (monsterAttack.getAttack() * monsterEffect.getMultiplier()));
                            }
                        }
                        case MONSTER_HEALTH -> {
                            switch (monsterEffect.getAction()) {
                                case ADD ->
                                        monster.setHp(min(monster.getMaxHp(), monster.getHp() + monsterEffect.getAmount()));
                                case MULTIPLY ->
                                        monster.setHp((int) min(monster.getMaxHp(), monster.getHp() * monsterEffect.getMultiplier()));
                            }
                        }
                    }
                    if (isFalse(monsterEffect.getHasFirstTurnPassed())) {
                        monsterEffect.setHasFirstTurnPassed(true);
                    }
                    monsterEffect.decreaseTurnsLasts();
                });
        return monster;
    }


    public Player updatePlayerEffects(Player player) {
        removeExpiredEffects(player);
        player.getEffects().forEach((attribute, playerEffects) -> {
            switch (attribute) {
                case HEALTH -> {
                    Integer value = summarizeEffects(playerEffects, player.getHp());
                    player.setHp(min(0, max(player.getMaxHp(), value)));
                }
                case MANA -> {
                    Integer value = summarizeEffects(playerEffects, player.getMana());
                    player.setMana(min(0, max(player.getMaxHp(), value)));
                }
                case ATTACK -> player.getInventory().getWeaponSet().getWeapons().forEach(weapon -> {
                    Integer value = summarizeEffects(playerEffects, weapon.getAttack());
                    weapon.setAttack(value);
                });
                case ARMOR -> {
                    Integer value = summarizeEffects(playerEffects, player.getMaxDefense());
                    player.setMaxDefense(value);
                }
                case CRITICAL_HIT_CHANCE -> player.getInventory().getWeaponSet().getWeapons().forEach(weapon -> {
                    Double value = summarizeEffects(playerEffects, weapon.getCriticalHitChance());
                    weapon.setCriticalHitChance(value);
                });
                case MISS_CHANCE -> player.getInventory().getWeaponSet().getWeapons().forEach(weapon -> {
                    Double value = summarizeEffects(playerEffects, weapon.getChanceToMiss());
                    weapon.setChanceToMiss(value);
                });
                case KNOCK_OUT_CHANCE -> player.getInventory().getWeaponSet().getWeapons().forEach(weapon -> {
                    Double value = summarizeEffects(playerEffects, weapon.getChanceToKnockOut());
                    weapon.setChanceToKnockOut(value);
                });
                case CHANCE_TO_DODGE -> {
                    Double value = summarizeEffects(playerEffects, player.getInventory().getArmorSet().getBoots().getChanceToDodge());
                    player.getInventory().getArmorSet().getBoots().setChanceToDodge(value);
                }
            }
        });
        return player;
    }

    private Double summarizeEffects(PriorityQueue<PlayerEffect> effects, Double initialValue) {
        val effectsMap = effects.stream()
                .collect(Collectors.groupingBy(Effect::getAction));
        return effectsMap.values().stream().mapToDouble(playerEffects -> playerEffects.stream()
                        .filter(e -> MULTIPLY.equals(e.getAction()))
                        .map(Effect::getMultiplier)
                        .reduce(initialValue, (m1, m2) -> m1 * m2))
                .findFirst().orElse(initialValue);
    }

    private Integer summarizeEffects(PriorityQueue<PlayerEffect> effects, Integer initialValue) {
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

    private boolean isApplicable(Effect effect) {
        if (!effect.getHasFirstTurnPassed()) {
            return true;
        }
        return !effect.isPermanent() && ((Expirable) effect).getIsAccumulated();
    }

    public void removeExpiredEffects(Monster monster) {
        val effectsToRemove = monster.getEffects().stream()
                .filter(monsterEffect -> !monsterEffect.isPermanent())
                .map(monsterEffect -> {
                    val turnsLeft = monsterEffect.decreaseTurnsLasts();
                    if (turnsLeft < 1) {
                        return monsterEffect;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
        monster.getEffects().removeAll(effectsToRemove);
    }

    public void removeExpiredEffects(Player player) {
        val effectsToRemove = player.getEffects().values().stream()
                .flatMap(Collection::stream)
                .filter(playerEffect -> playerEffect instanceof Expirable)
                .map(playerEffect -> {
                    val turnsLeft = ((Expirable) playerEffect).decreaseTurnsLasts();
                    if (turnsLeft < 1) {
                        return playerEffect;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
        effectsToRemove.forEach(playerEffect -> player.getEffects()
                .get(playerEffect.getAttribute())
                .remove(playerEffect));
        savePlayerEffects(player.getEffects().values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
    }

    public void savePlayerEffects(List<PlayerEffect> effects) {
        val documents = EffectMapper.INSTANCE.mapToDocuments(effects.stream().map(playerEffect -> (Effect) playerEffect).collect(Collectors.toList()));
        effectRepository.saveAll(documents);
    }
    public PlayerEffect savePlayerEffect(PlayerEffect effect) {
        val document = EffectMapper.INSTANCE.mapToDocument(effect);
        return EffectMapper.INSTANCE.mapToPlayerEffect(effectRepository.save(document));
    }

    public List<ItemEffect> saveItemEffects(List<ItemEffect> effects) {
        List<EffectDocument> documents = EffectMapper.INSTANCE.mapToDocuments(effects.stream().map(itemEffect -> (Effect) itemEffect).collect(Collectors.toList()));
        val savedItemEffectDocuments = effectRepository.saveAll(documents);
        return EffectMapper.INSTANCE.mapToItemEffects(savedItemEffectDocuments);
    }
}
