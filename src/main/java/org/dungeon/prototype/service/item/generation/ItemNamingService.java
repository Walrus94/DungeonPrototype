package org.dungeon.prototype.service.item.generation;

import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.kafka.KafkaProducer;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.items.naming.api.dto.ItemNameRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Slf4j
@Component
public class ItemNamingService {
    private static final String DEFAULT_ITEM_NAME = "Mysterious unnamed item";

    @Value("${kafka-topics.item-naming-topic}")
    private String topic;
    @Autowired
    KafkaProducer kafkaProducer;

    /**
     * Sends request to generate name for item
     *
     * @param item to be named
     */
    public void requestNameGeneration(Item item) {
        log.debug("Preparing item {} for naming request...", item.getId());
        kafkaProducer.sendItemNamingRequest(topic,
                new ItemNameRequestDto(item.getChatId(), item.getId(), generatePrompt(item)));
    }

    private String generatePrompt(Item item) {
        return item.getAttributes().toString() +
                (nonNull(item.getEffects()) && !item.getEffects().isEmpty() ?
                        " that " + item.getEffects().stream().map(Effect::toString).collect(Collectors.joining(", ")) :
                        "");

    }
}
