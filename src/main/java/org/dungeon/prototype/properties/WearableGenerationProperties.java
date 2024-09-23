package org.dungeon.prototype.properties;

import lombok.Data;
import org.dungeon.prototype.model.inventory.attributes.Quality;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Map;

@Data
@ConfigurationPropertiesScan
@ConfigurationProperties("generation.items.wearables")
public class WearableGenerationProperties {
    private Map<WearableMaterial, Integer> armorBonus;
    private Map<Quality, Double> qualityAdjustmentRatio;
    private Map<WearableMaterial, Double> chanceToDodgeRatio;

}
