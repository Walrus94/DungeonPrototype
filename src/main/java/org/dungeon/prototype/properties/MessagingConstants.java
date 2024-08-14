package org.dungeon.prototype.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Map;

@Data
@ConfigurationPropertiesScan
@ConfigurationProperties("messaging.constants")
public class MessagingConstants {
    Map<Emoji, String> emoji;
}
