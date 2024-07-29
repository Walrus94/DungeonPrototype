package org.dungeon.prototype.model.inventory.items.naming.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ItemNameRequestDto {
    private List<String> prompts;
}
