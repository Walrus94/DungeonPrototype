package org.dungeon.prototype.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@Data
@ConfigurationPropertiesScan
@ConfigurationProperties("generation.items")
public class ItemsGenerationProperties {
    private WeaponGenerationProperties weapon;
    private WearableGenerationProperties wearables;
    private ItemEffectsGenerationProperties effects;
}
