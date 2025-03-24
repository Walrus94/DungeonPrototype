package org.dungeon.prototype.service.item.generation;

import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.kafka.KafkaProducer;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.kafka.request.naming.ItemNameRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Slf4j
@Component
public class ItemNamingService {
    @Autowired
    private KafkaProducer kafkaProducer;

    /**
     * Sends request to generate name for item
     *
     * @param item to be named
     */
    public Item requestNameGeneration(Item item) {
        log.info("Preparing item {} for naming request...", item.getId());
        kafkaProducer.sendItemNamingRequest(
                new ItemNameRequest(item.getChatId(), item.getId(), generatePrompt(item)));
        item.setHfRequestSent(true);
        return item;
    }

    private String generatePrompt(Item item) {
        return item.getAttributes().toString() +
                (nonNull(item.getEffects()) && !item.getEffects().isEmpty() ?
                        " that " + item.getEffects().stream().map(Effect::toString).collect(Collectors.joining(", ")) :
                        "");
    }
}
