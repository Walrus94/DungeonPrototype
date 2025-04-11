package org.dungeon.prototype.model.kafka.request.naming;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.dungeon.prototype.model.kafka.request.KafkaMessage;

@Data
@AllArgsConstructor
public class ItemNameRequest implements KafkaMessage {
    @JsonProperty("chatId")
    private long chatId;
    @JsonProperty("id")
    private String id;
    @JsonProperty("prompt")
    private String prompt;

    @Override
    public String getData() {
        return prompt;
    }
}
