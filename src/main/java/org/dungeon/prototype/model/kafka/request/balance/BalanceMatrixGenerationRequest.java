package org.dungeon.prototype.model.kafka.request.balance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BalanceMatrixGenerationRequest {
    @JsonProperty("name")
    private String name;
    @JsonProperty("cols")
    private int cols;
    @JsonProperty("rows")
    private int rows;
}
