package org.dungeon.prototype.model.inventory.items;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.usable.UsableAttributes;

import static org.dungeon.prototype.model.document.item.ItemType.USABLE;

@Data
@EqualsAndHashCode(callSuper = false)
public class Usable extends Item {
    UsableAttributes attributes;
    Integer amount;
    @Override
    public ItemType getItemType() {
        return USABLE;
    }
}
