package org.dungeon.prototype.model.monster;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.util.MonsterGenerationUtil;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class Monster {
    private String id;
    private MonsterClass monsterClass;
    private Integer level;

    private Integer hp;
    private Integer maxHp;
    private Integer xpReward;

    private MonsterAttack primaryAttack;
    private MonsterAttack secondaryAttack;

    private Iterator<MonsterAttack> currentAttack;
    public List<MonsterAttack> getDefaultAttackPattern() {
        return MonsterGenerationUtil.getDefaultAttackPattern().stream().mapToObj(value -> {
            if (value == 1) {
                return getSecondaryAttack();
            } else {
                return getPrimaryAttack();
            }
        }).collect(Collectors.toList());
    }

    public Integer getWeight() {
        return -xpReward; //TODO: consider different formula
    }

    public void decreaseHp(Integer amount) {
        hp -= amount;
    }
}
