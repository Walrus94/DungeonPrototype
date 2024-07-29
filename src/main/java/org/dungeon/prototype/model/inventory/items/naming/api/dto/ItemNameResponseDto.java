package org.dungeon.prototype.model.inventory.items.naming.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ItemNameResponseDto {
    @JsonProperty("prompt")
    private String prompt;
    @JsonProperty("response")
    private String response;
}
