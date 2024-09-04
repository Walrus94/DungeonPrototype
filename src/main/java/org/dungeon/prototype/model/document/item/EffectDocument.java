package org.dungeon.prototype.model.document.item;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.effect.attributes.Action;
import org.dungeon.prototype.model.effect.attributes.EffectApplicant;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "effects")
@NoArgsConstructor
public class EffectDocument {
    @Id
    private String id;
    private EffectApplicant applicableTo;
    private EffectAttribute attribute;
    private Boolean isPermanent;
    private Boolean isAccumulated;
    private Action action;
    private Integer turnsLasts;
    private Integer amount;
    private Integer baseAmount;
    private Double multiplier;
    private Integer weight;
}
