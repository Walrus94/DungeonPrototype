package org.dungeon.prototype.service.inventory;

import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ItemNamingService {

    @Value("${openai.token}")
    private String token;

    @Autowired
    private OpenAiService openAiService;


}
