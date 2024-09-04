package org.dungeon.prototype.model.inventory;

import lombok.Data;
import org.dungeon.prototype.model.document.item.ItemAttributes;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.effect.PermanentEffect;
import org.dungeon.prototype.model.inventory.attributes.MagicType;

import java.util.List;

@Data
public abstract class Item {
    protected String id;
    protected String name;
    protected Long chatId;
    protected Integer weight;
    protected Integer buyingPrice;
    protected Integer sellingPrice;
    protected List<PermanentEffect> effects;
    protected Boolean hasMagic;
    protected MagicType magicType;
    public abstract ItemAttributes getAttributes();
    public abstract ItemType getItemType();
}
