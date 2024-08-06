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
        private Integer primaryAttackRatio;
        private Integer primaryAttackBonus;
        private Integer secondaryAttackRatio;
        private Integer secondaryAttackBonus;
        private Integer healthRatio;
        private Integer healthBonus;
        private Integer weightPrimaryAttackMultiplier;
        private Integer weightSecondaryAttackMultiplier;
    }

    @Data
    public static class LevelGenerationAttributes {
        private Integer levelOneGridSize;
        private Integer gridSizeIncrement;
        private Integer incrementStep;
        private Double maxLengthRatio;
        private Double minLengthRatio;
        private Integer minLength;
    }
}
