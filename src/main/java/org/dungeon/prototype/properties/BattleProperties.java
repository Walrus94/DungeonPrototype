package org.dungeon.prototype.properties;

import lombok.Data;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial;
import org.dungeon.prototype.model.monster.MonsterAttackType;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Map;

@Data
@ConfigurationPropertiesScan
@ConfigurationProperties(prefix = "battle")
public class BattleProperties {
    Map<MonsterAttackType, MaterialDefenseRatioMap> playerDefenseRatioMatrix;
    Map<WeaponAttackType, MonsterDefenseRatioMap> monsterDefenseRatioMatrix;
    Map<MonsterClass, PlayerAttackRatioMap> playerAttackMatrixTemplate;
    Map<WearableMaterial, MonsterAttackRatioMap> monsterAttackMatrixTemplate;

    @Data
    public static class MaterialDefenseRatioMap {
        Map<WearableMaterial, Double> materialDefenseRatioMap;
    }

    @Data
    public static class MonsterDefenseRatioMap {
        Map<MonsterClass, Double> monsterDefenseRatioMap;
    }

    @Data
    public static class PlayerAttackRatioMap {
        Map<WeaponAttackType, Double> playerAttackRatioMap;
    }

    @Data
    public static class MonsterAttackRatioMap {
        Map<MonsterAttackType, Double> monsterAttackRatioMap;
    }
}
