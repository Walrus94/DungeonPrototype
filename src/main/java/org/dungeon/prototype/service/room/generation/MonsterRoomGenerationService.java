package org.dungeon.prototype.service.room.generation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.properties.GenerationProperties;
import org.dungeon.prototype.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.dungeon.prototype.util.RoomGenerationUtils.convertToRoomType;

@Slf4j
@Service
public class MonsterRoomGenerationService {
    @Autowired
    private GenerationProperties generationProperties;
    private Pair<Integer, Integer> getMonsterClassWeightRangeByLevel(MonsterClass monsterClass, Integer level) {
        val properties = generationProperties.getMonsters().get(monsterClass);
        val start = level * (properties.getWeightPrimaryAttackMultiplier() * properties.getPrimaryAttackRatio() +
                properties.getWeightSecondaryAttackMultiplier() * properties.getSecondaryAttackRatio() + properties.getHealthRatio());
        val end = start + properties.getWeightPrimaryAttackMultiplier() * properties.getPrimaryAttackBonus() +
                properties.getWeightSecondaryAttackMultiplier() * properties.getSecondaryAttackBonus() +
                properties.getHealthBonus();
        return Pair.create(start, end);
    }

    public Integer getMonsterClassLevelByWeight(MonsterClass monsterClass, Integer weight) {
        log.debug("Getting available levels for {} of weight {}...", monsterClass, weight);
        var levels = new ArrayList<Integer>();
        var level = 1;
        var range = getMonsterClassWeightRangeByLevel(monsterClass, level);
        while (range.getFirst() < weight) {
            log.debug("Level {} range: {}", level, range);
            if (range.getSecond() > weight) {
                levels.add(level);
                log.debug("Level {} added", level);
            }
            level++;
            range = getMonsterClassWeightRangeByLevel(monsterClass, level);
        }
        if (levels.isEmpty()) {
            return 1;
        }
        return levels.get(RandomUtil.getRandomInt(0, levels.size() - 1));
    }

    public Set<RoomType> getExcludedMonsters(Integer expectedWeightAbs) {
        return Arrays.stream(MonsterClass.values()).map(monsterClass -> {
            var currentLevel = 1;
            var range = getMonsterClassWeightRangeByLevel(monsterClass, currentLevel);
            if (range.getFirst() > expectedWeightAbs) {
                return monsterClass;
            }
            while (range.getFirst() < expectedWeightAbs) {
                if (range.getSecond() < expectedWeightAbs) {
                    currentLevel++;
                    range = getMonsterClassWeightRangeByLevel(monsterClass, currentLevel);
                } else {
                    return null;
                }
            }
            return monsterClass;
        }).filter(Objects::nonNull).map(monsterClass -> convertToRoomType(monsterClass, true)).collect(Collectors.toSet());
    }
}
