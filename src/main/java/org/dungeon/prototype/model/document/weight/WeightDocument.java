package org.dungeon.prototype.model.document.weight;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "weight")
@NoArgsConstructor
public class WeightDocument {
    @Id
    private String id;
    private Double hp;
    private Double maxHp;
    private Double mana;
    private Double maxMana;
    private Double armor;
    private Double maxArmor;
    private Double chanceToDodge;
    private Double goldBonus;
    private Double xpBonus;
    private Double attack;
    private Double criticalHitChance;
    private Double criticalHitMultiplier;
    private Double chanceToKnockout;
    private Double arcaneMagic;
    private Double divineMagic;
}
