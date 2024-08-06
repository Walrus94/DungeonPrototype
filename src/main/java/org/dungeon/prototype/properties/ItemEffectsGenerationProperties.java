package org.dungeon.prototype.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Map;

@Data
@ConfigurationPropertiesScan
@ConfigurationProperties("generation.items.effects")
public class ItemEffectsGenerationProperties {
    private Integer minimumAmountPerItem;
    private Integer maximumAmountPerItem;
    private Map<Integer, Double> randomEffectAdditionMap;
    private Map<Double, Double> randomEffectMultiplierMap;
    private Integer weightAdditionRatio;
    private Integer weightMultiplierRatio;
}
