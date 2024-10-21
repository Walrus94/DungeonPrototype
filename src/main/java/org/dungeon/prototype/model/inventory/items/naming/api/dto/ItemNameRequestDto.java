package org.dungeon.prototype.model.inventory.items.naming.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemNameRequestDto {
    @JsonProperty("chatId")
    private Long chatId;
    @JsonProperty("id")
    private String id;
    @JsonProperty("prompt")
    private String prompt;
}
