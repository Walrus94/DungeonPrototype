package org.dungeon.prototype.model.inventory;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.item.ItemAttributes;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.dungeon.prototype.model.weight.Weight;

import java.util.List;

import static org.dungeon.prototype.util.GenerationUtil.getBuyingPriceRatio;
import static org.dungeon.prototype.util.GenerationUtil.getSellingPriceRatio;

@Data
@NoArgsConstructor
public abstract class Item {
    protected String id;
    protected String name;
    protected Long chatId;
    protected List<Effect> effects;
    protected MagicType magicType;
    public abstract ItemAttributes getAttributes();
    public abstract ItemType getItemType();
    public abstract Weight getWeight();

    public Integer getBuyingPrice() {
        return (int) (getWeight().toVector().getNorm() * getBuyingPriceRatio());
    }

    public Integer getSellingPrice() {
        return (int) (getWeight().toVector().getNorm() * getSellingPriceRatio());
    }
    public Item(Item item) {
        this.name = item.getName();
        this.chatId = item.getChatId();
        this.effects = item.getEffects();
        this.magicType = item.getMagicType();
    }
}
