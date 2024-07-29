package org.dungeon.prototype.model.room.content;

import lombok.Data;
import org.dungeon.prototype.model.inventory.Item;

import java.util.Set;

@Data
public abstract class BonusRoom implements RoomContent {
    protected String id;
    protected Set<Item> items;
}
