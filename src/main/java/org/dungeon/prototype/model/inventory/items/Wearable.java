package org.dungeon.prototype.model.inventory.items;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.effect.PermanentEffect;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Wearable extends Item {
    private WearableAttributes attributes;

    private Integer armor;
    private Double chanceToDodge;
    private List<PermanentEffect> effects;

    @Override
    public ItemType getItemType() {
        return ItemType.WEARABLE;
    }
}
