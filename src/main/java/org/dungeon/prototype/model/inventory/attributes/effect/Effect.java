package org.dungeon.prototype.model.inventory.attributes.effect;

import lombok.Data;
import org.dungeon.prototype.model.inventory.Item;

@Data
public class Effect {
    private String id;
    private Class<? extends Item> applicableTo;
    private Attribute attribute;
    private Action action;
    private Boolean isPermanent;
    private Integer turnsLasts;
    private Integer amount;
    private Double multiplier;
    private Integer weight;
}
