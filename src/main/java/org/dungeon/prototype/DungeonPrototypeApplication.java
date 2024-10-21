package org.dungeon.prototype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.starter.TelegramBotStarterConfiguration;

@SpringBootApplication
@Import(TelegramBotStarterConfiguration.class)
@ConfigurationPropertiesScan("org.dungeon.prototype.properties")
@EnableAspectJAutoProxy
@EnableScheduling
public class DungeonPrototypeApplication {
    public static void main(String[] args) {
        SpringApplication.run(DungeonPrototypeApplication.class, args);
    }
}