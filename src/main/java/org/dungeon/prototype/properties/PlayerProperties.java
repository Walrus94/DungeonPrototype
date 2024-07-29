package org.dungeon.prototype.properties;

import lombok.Data;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.EnumMap;

@Data
@ConfigurationPropertiesScan
@ConfigurationProperties("player")
public class PlayerProperties {
    EnumMap<PlayerAttribute, Integer> attributes;
}
