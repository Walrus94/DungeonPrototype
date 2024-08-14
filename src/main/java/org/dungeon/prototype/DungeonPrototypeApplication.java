package org.dungeon.prototype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@ConfigurationPropertiesScan("org.dungeon.prototype.properties")
@EnableAspectJAutoProxy
public class DungeonPrototypeApplication {
    public static void main(String[] args) {
        SpringApplication.run(DungeonPrototypeApplication.class, args);
    }
}