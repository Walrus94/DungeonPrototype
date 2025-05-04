package org.dungeon.prototype.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.exception.KafkaMessageException;
import org.dungeon.prototype.model.kafka.request.balance.BalanceMatricesRequest;
import org.dungeon.prototype.model.kafka.request.naming.ItemNameRequest;
import org.dungeon.prototype.model.stats.GameResult;
import org.dungeon.prototype.properties.CallbackType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaProducer {
    @Value("${kafka-topics.item-naming-topic}")
    private String itemNamingTopic;
    @Value("${kafka-topics.balance-matrix-topic}")
    private String balanceMatrixTopic;
    @Value("${kafka-topics.game-results-topic}")
    private String gameResultsTopic;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    public void sendItemNamingRequest(ItemNameRequest dto) {
        String message;
        try {
            message = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new KafkaMessageException(dto, CallbackType.MENU_BACK);
        }
        log.info("Sending message to kafka stream, topic: {}, message: {}", itemNamingTopic, message);
        kafkaTemplate.send(itemNamingTopic, message);
    }

    public void sendBalanceMatrixGenerationRequest(BalanceMatricesRequest request) {
        String message;
        try {
            message = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new KafkaMessageException(request, CallbackType.MENU_BACK);
        }
        log.info("Sending message to kafka stream, topic: {}, message: {}", balanceMatrixTopic, message);
        kafkaTemplate.send(balanceMatrixTopic, message);
    }

    public void sendGameResults(GameResult gameResult) {
        String message;
        try {
            message = objectMapper.writeValueAsString(gameResult);
        } catch (JsonProcessingException e) {
            throw new KafkaMessageException(gameResult, CallbackType.MENU_BACK);
        }
        log.info("Sending message to kafka stream, topic: {}, message: {}", gameResultsTopic, message);
        kafkaTemplate.send(gameResultsTopic, message);
    }
}
