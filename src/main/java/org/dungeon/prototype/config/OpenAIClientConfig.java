package org.dungeon.prototype.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIClientConfig {
    @Bean
    public OpenAiService OpenAiService(@Value("openai.token") String token) {
        return new OpenAiService(token);
    }
}
