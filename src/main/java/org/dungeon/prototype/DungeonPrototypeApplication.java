package org.dungeon.prototype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("org.dungeon.prototype.properties")
public class DungeonPrototypeApplication {
    public static void main(String[] args) {
        SpringApplication.run(DungeonPrototypeApplication.class, args);
    }
}