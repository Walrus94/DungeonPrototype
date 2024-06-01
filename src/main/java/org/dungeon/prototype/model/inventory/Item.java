package org.dungeon.prototype.model.inventory;

public interface Item {
    String getName();
    Integer getWeight();
    Integer getBuyingPrice();
    Integer getSellingPrice();
    boolean hasRoomForGem();
    default boolean isWearable() {
        return this instanceof Wearable;
    }
}
