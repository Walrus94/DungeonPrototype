package org.dungeon.prototype.model.inventory.attributes;

import org.dungeon.prototype.model.inventory.Item;

import java.util.List;

public interface Attribute {
     List<Item> applicableTo();
     Effect getEffects();
}
