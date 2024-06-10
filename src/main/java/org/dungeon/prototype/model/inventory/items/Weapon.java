package org.dungeon.prototype.model.inventory.items;

import lombok.AllArgsConstructor;
import org.dungeon.prototype.model.inventory.Item;

@AllArgsConstructor
public class Weapon implements Item {
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Integer getWeight() {
        return getAttack(); //TODO: investigate calculating weight for weapons
    }

    @Override
    public Integer getBuyingPrice() {
        return 5;
    }

    @Override
    public Integer getSellingPrice() {
        return 0;
    }

    @Override
    public boolean hasRoomForGem() {
        return false;
    }

    public enum Handling {
        SINGLE_HANDED, TWO_HANDED, ADDITIONAL
    }

    public enum HandlerMaterial {
        WOOD, STEEL, LEATHER, RUBBER
    }

    public enum Material {
        WOOD, STEEL, PLATINUM, DIAMOND, ELEMENTAL
    }

    public enum Size {
        SMALL, MEDIUM, LARGE
    }

    public enum Action {
        PIERCE, SLASH, STUB, FIRE, DEFENSE, HEAL
    }

    private Handling handling;
    private String name;
    private Integer attack;
    private Integer sellingPrice;
    private Integer priceDelta;

    public Handling getHandling() {
        return handling;
    }

    public Integer getAttack() {
        return attack;
    }
}
