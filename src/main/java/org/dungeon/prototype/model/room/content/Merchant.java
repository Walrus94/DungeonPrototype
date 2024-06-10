package org.dungeon.prototype.model.room.content;

import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.room.RoomContent;
import org.dungeon.prototype.model.room.RoomType;

import java.util.Collections;
import java.util.List;

public class Merchant implements RoomContent {

    List<Item> items = Collections.emptyList();

    @Override
    public Integer getRoomContentWeight() {
        return 400;//items.stream().mapToInt(Item::getWeight).sum();
    }
    @Override
    public RoomType getRoomType() {
        return RoomType.MERCHANT;
    }
}
