package org.dungeon.prototype.model.kafka.request;

public interface KafkaMessage {
    long getChatId();
    String getData();
}
