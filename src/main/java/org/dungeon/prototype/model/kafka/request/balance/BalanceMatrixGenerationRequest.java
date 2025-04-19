package org.dungeon.prototype.model.kafka.request.balance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.dungeon.prototype.model.kafka.request.KafkaMessage;

@Data
@AllArgsConstructor
public class BalanceMatrixGenerationRequest implements KafkaMessage {
    @JsonProperty("chatId")
    private long chatId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("cols")
    private int cols;
    @JsonProperty("rows")
    private int rows;


    @Override
    public String getData() {
        return name;
    }
}
