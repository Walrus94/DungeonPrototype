package org.dungeon.prototype.model.document.monster;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.item.EffectDocument;
import org.dungeon.prototype.model.document.weight.WeightDocument;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.dungeon.prototype.model.monster.MonsterAttack;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "monsters")
public class MonsterDocument {
    @Id
    private String id;
    private MonsterClass monsterClass;
    private MonsterAttack primaryAttack;
    private MonsterAttack secondaryAttack;
    private MagicType magicType;
    private Integer maxHp;
    private Integer hp;
    private WeightDocument weight;

    private LinkedList<MonsterAttack> attackPattern;
    private MonsterAttack currentAttack;

    private List<EffectDocument> effects;

}
