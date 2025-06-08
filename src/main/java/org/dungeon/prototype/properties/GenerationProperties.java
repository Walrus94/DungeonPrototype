package org.dungeon.prototype.properties;

import lombok.Data;
import org.dungeon.prototype.model.monster.MonsterAttackType;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Map;

@Data
@ConfigurationPropertiesScan
@ConfigurationProperties(prefix = "generation")
public class GenerationProperties {
    private Map<MonsterClass, MonsterClassGenerationAttributes> monsters;
    private LevelGenerationAttributes level;
    private ItemsGenerationProperties items;

    @Data
    public static class MonsterClassGenerationAttributes {
        private MonsterAttackType primaryAttackType;
        private MonsterAttackType secondaryAttackType;
        private Double primeToSecAttackWeights;
    }

    @Data
    public static class LevelGenerationAttributes {
        private int levelOneGridSize;
        private int gridSizeIncrement;
        private int incrementStep;
        private int clusterGenerationRetries = 1;
    }
}
