package org.dungeon.prototype.aspect.dto;

import lombok.Builder;
import lombok.Data;
import org.dungeon.prototype.properties.CallbackType;
import org.springframework.lang.Nullable;

@Data
@Builder
public class InventoryItemResponseDto {
    private Long chatId;
    private String itemId;
    private CallbackType inventoryType;
    @Nullable
    private String itemType;
    @Builder.Default
    private boolean isOk = true;
}
