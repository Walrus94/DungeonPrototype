package org.dungeon.prototype.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.exception.KafkaMessageException;
import org.dungeon.prototype.model.inventory.items.naming.api.dto.ItemNameRequestDto;
import org.dungeon.prototype.properties.CallbackType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaProducer {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    public void sendItemNamingRequest(String topic, ItemNameRequestDto dto) {
        String message;
        try {
            message = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new KafkaMessageException(dto, CallbackType.MENU_BACK);
        }
        log.info("Sending message to kafka stream, topic: {}, message: {}", topic, message);
        kafkaTemplate.send(topic, message);
    }
}
