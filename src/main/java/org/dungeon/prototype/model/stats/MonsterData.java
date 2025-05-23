package org.dungeon.prototype.model.stats;

import lombok.Data;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.weight.Weight;

@Data
public class MonsterData {
    private MonsterClass monsterClass;
    private Weight weight;
    private int stepKilled;
    private int battleSteps;

    public MonsterData(MonsterClass monsterClass, Weight weight) {
        this.monsterClass = monsterClass;
        this.weight = weight;
        this.stepKilled = -1;
        this.battleSteps = 0;
    }

    public void incrementSteps() {
        battleSteps++;
    }
}
