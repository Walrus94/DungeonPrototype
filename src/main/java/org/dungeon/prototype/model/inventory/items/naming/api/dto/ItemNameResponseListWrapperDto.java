package org.dungeon.prototype.model.inventory.items.naming.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ItemNameResponseListWrapperDto {
    private List<ItemNameResponseDto> responses;
}
