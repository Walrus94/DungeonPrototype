package org.dungeon.prototype.model.kafka.request.balance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.dungeon.prototype.model.kafka.request.KafkaMessage;

import java.util.List;

@Data
@AllArgsConstructor
public class BalanceMatricesRequest implements KafkaMessage {
    @JsonProperty("chatId")
    private long chatId;
    @JsonProperty("data")
    List<BalanceMatrixGenerationRequest> data;
}
