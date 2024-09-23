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
    private Double hpToMaxHp;
    private Double hpDeficiencyToMaxHp;
    private Double manaToMaxMana;
    private Double manaDeficiencyToMaxMana;
    private Double armorToMaxArmor;
    private Double armorDeficiencyToMaxArmor;
    private Double chanceToDodge;
    private Double goldBonusToGold;
    private Double xpBonus;
    private Double attack;
    private Double criticalHitChance;
    private Double criticalHitMultiplier;
    private Double chanceToKnockout;
    private Double arcaneMagic;
    private Double divineMagic;
}
