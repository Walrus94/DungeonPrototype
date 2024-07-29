package org.dungeon.prototype.model.monster;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class MonsterAttack {
    private MonsterAttackType attackType;
    private Integer attack;
}
