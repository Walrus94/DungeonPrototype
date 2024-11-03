package org.dungeon.prototype.service.generation;

import org.dungeon.prototype.config.BotConfig;
import org.dungeon.prototype.config.TestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {BotConfig.class, TestConfig.class})
@ActiveProfiles("test")
public class LevelGenerationServiceTest {
}
