package org.dungeon.prototype.exception;

import org.dungeon.prototype.model.inventory.items.naming.api.dto.ItemNameRequestDto;
import org.dungeon.prototype.properties.CallbackType;

public class KafkaMessageException extends PlayerException {
    public KafkaMessageException(ItemNameRequestDto dto, CallbackType buttonData) {
        super(String.format("Unable to request name for item:%s, prompt:%s", dto.getId(), dto.getPrompt()),
                dto.getChatId(), buttonData);
    }
}
