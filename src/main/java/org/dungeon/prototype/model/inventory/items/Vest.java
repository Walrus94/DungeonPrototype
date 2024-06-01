package org.dungeon.prototype.model.inventory.items;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dungeon.prototype.model.inventory.Wearable;
import org.dungeon.prototype.model.inventory.attributes.Attribute;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class Vest extends Wearable {

    private List<Attribute> attributes;
    private String name;
    private Integer armor;

    private Integer sellingPrice;
    private Integer buyingPrice;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Integer getWeight() {
        return null;
    }

    @Override
    public Integer getBuyingPrice() {
        return buyingPrice;
    }

    @Override
    public Integer getSellingPrice() {
        return sellingPrice;
    }

    @Override
    public boolean hasRoomForGem() {
        return false;
    }

    @Override
    public Integer getArmor() {
        return armor;
    }
}
