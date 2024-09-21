package org.dungeon.prototype.model.inventory.items;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.usable.UsableAttributes;
import org.dungeon.prototype.model.weight.Weight;

import static org.dungeon.prototype.model.document.item.ItemType.USABLE;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Usable extends Item {
    UsableAttributes attributes;
    Integer amount;
    @Override
    public ItemType getItemType() {
        return USABLE;
    }

    @Override
    public Weight getWeight() {
        return effects.stream().map(Effect::getWeight).reduce(Weight::add).orElse(new Weight()).multiply(Double.valueOf(amount));
    }

    public Usable(Usable usable) {
        super(usable);
        this.attributes = usable.getAttributes();
        this.amount = usable.getAmount();
    }
}
