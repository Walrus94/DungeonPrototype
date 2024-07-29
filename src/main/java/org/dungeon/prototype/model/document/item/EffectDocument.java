package org.dungeon.prototype.model.document.item;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.inventory.attributes.effect.Action;
import org.dungeon.prototype.model.inventory.attributes.effect.Attribute;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "item")
@NoArgsConstructor
public class EffectDocument {
    @Id
    private String id;
    private String applicableTo;
    private Attribute attribute;
    private Action action;
    private Boolean isPermanent;
    private Integer turnsLasts;
    private Integer amount;
    private Double multiplier;
    private Integer weight;
}
