package org.dungeon.prototype.model.inventory.items;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes;
import org.dungeon.prototype.model.weight.Weight;

import static java.util.Objects.nonNull;
import static org.dungeon.prototype.util.GenerationUtil.calculateWearableWeight;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Wearable extends Item {
    private WearableAttributes attributes;

    private Integer armor;
    private Double chanceToDodge;

    @Override
    public ItemType getItemType() {
        return ItemType.WEARABLE;
    }
    public Weight getWeight() {
        return calculateWearableWeight(armor, (nonNull(chanceToDodge) ? chanceToDodge : 0.0), magicType)
                .add(effects.stream().map(Effect::getWeight).reduce(Weight::add)
                        .orElse(new Weight()));
    }

    public Wearable(Wearable wearable) {
        super(wearable);
        this.attributes = wearable.getAttributes();
        this.chanceToDodge = wearable.getChanceToDodge();
        this.armor = wearable.getArmor();
    }
}
