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
}
