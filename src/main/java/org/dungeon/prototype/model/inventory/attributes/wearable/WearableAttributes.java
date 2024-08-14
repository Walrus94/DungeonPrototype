package org.dungeon.prototype.model.inventory.attributes.wearable;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.item.ItemAttributes;
import org.dungeon.prototype.model.inventory.attributes.Quality;

@Data
@NoArgsConstructor
public class WearableAttributes implements ItemAttributes {
    private WearableType wearableType;
    private Quality quality;
    private WearableMaterial wearableMaterial;

    @Override
    public String toString() {
        return quality + " "
                + wearableType + " made of " +
                wearableMaterial;
    }
}
