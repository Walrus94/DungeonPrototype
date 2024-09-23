package org.dungeon.prototype.model.document.item;

import org.dungeon.prototype.model.inventory.attributes.Quality;

public interface ItemAttributes {
    Quality getQuality();
    String toString();
}
