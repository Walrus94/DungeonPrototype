package org.dungeon.prototype.model.document.item;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "items")
@NoArgsConstructor
public class ItemDocument {
    @Id
    private String id;
    @Indexed
    private Long chatId;
    private ItemAttributes attributes;
    private ItemType itemType;

    //weapon specs
    private Integer attack;
    private Double criticalHitChance;
    private Double criticalHitMultiplier;
    private Double chanceToMiss;
    private Double chanceToKnockOut;
    private Boolean isCompleteDragonBone;

    //wearable specs
    private Integer armor;
    private Double chanceToDodge;

    //usable specs
    private Integer amount;
    private String name;
    @Indexed
    private Integer weight;
    private boolean hasMagic;
    private MagicType magicType;
    private List<EffectDocument> effects = new ArrayList<>();

    private Integer sellingPrice;
    private Integer buyingPrice;
}
