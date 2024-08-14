package org.dungeon.prototype.model.monster;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class MonsterAttack {
    private MonsterAttackType attackType;
    private Integer attack;
}
