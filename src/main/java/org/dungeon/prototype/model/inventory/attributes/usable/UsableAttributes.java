package org.dungeon.prototype.model.inventory.attributes.usable;

import lombok.Data;
import org.dungeon.prototype.model.document.item.ItemAttributes;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.inventory.attributes.Quality;

import java.util.List;

@Data
public class UsableAttributes implements ItemAttributes {
    List<Effect> effects;
    Quality quality;
    @Override
    public Quality getQuality() {
        return quality;
    }
}
