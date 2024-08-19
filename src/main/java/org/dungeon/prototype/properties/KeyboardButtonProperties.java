package org.dungeon.prototype.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Map;

@Data
@ConfigurationPropertiesScan
@ConfigurationProperties("messaging.keyboard")
public class KeyboardButtonProperties {
    private Map<CallbackType, KeyboardButtonAttributes> buttons;
    @Data
    public static class KeyboardButtonAttributes {
        private String name;
        private String callback;
    }
}
