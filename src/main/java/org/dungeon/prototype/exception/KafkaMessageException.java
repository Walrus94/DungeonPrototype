package org.dungeon.prototype.exception;

import org.dungeon.prototype.model.kafka.request.KafkaMessage;
import org.dungeon.prototype.properties.CallbackType;

public class KafkaMessageException extends PlayerException {
    public KafkaMessageException(KafkaMessage message, CallbackType buttonData) {
        super(String.format("Unable to send message: %s", message.getData()), message.getChatId(), buttonData);
    }
}
