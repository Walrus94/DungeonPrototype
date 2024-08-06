package org.dungeon.prototype.model.inventory.attributes.usable;

import lombok.Data;
import org.dungeon.prototype.model.document.item.ItemAttributes;
import org.dungeon.prototype.model.effect.DirectPlayerEffect;

import java.util.List;

@Data
public class UsableAttributes implements ItemAttributes {
    List<DirectPlayerEffect> effects;
}
