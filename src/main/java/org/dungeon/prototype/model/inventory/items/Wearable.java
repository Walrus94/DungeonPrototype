package org.dungeon.prototype.model.inventory.items;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.effect.Effect;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes;

import java.util.List;

@Data
@NoArgsConstructor
public class Wearable implements Item {
    private String id;
    private Long chatId;
    private WearableAttributes attributes;
    private String name;
    private Integer armor;
    private Double chanceToDodge;
    private boolean hasMagic;
    private MagicType magicType;
    private List<Effect> effects;
    private Integer weight;

    private Integer sellingPrice;
    private Integer buyingPrice;

    @Override
    public ItemType getItemType() {
        return ItemType.WEARABLE;
    }
}
