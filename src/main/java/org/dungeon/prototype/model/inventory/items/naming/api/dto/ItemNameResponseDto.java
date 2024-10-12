package org.dungeon.prototype.model.inventory.items.naming.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ItemNameResponseDto {
    String id;
    String object;
    Long created;
    String model;
    List<Choice> choices;
    Usage usage;

    @Data
    public static class Choice {
        int index;
        Message message;
        String finish_reason;
    }

    @Data
    public static class Usage {
        int prompt_tokens;
        int completion_tokens;
        int total_tokens;
    }
}
