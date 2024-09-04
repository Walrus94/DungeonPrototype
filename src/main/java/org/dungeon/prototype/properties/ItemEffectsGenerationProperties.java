package org.dungeon.prototype.properties;

import lombok.Data;
import org.dungeon.prototype.model.inventory.attributes.Quality;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Map;

@Data
@ConfigurationPropertiesScan
@ConfigurationProperties("generation.items.effects")
public class ItemEffectsGenerationProperties {
    private Map<Quality, Integer> minimumAmountPerItemMap;
    private Map<Quality, Integer> maximumAmountPerItemMap;
    private Map<Integer, Double> randomEffectAdditionMap;
    private Map<Double, Double> randomEffectMultiplierMap;
    private Integer weightAdditionRatio;
    private Integer weightMultiplierRatio;
}
