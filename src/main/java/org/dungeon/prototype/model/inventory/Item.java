package org.dungeon.prototype.model.inventory;

import org.dungeon.prototype.model.document.item.ItemAttributes;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.inventory.attributes.MagicType;

public interface Item {
    String getId();
    ItemType getItemType();
    String getName();
    Long getChatId();
    ItemAttributes getAttributes();
    Integer getWeight();
    boolean isHasMagic();
    MagicType getMagicType();
//    List<Effect> getEffects();
    Integer getBuyingPrice();
    Integer getSellingPrice();
}
