package org.dungeon.prototype.properties;

import lombok.Data;
import org.dungeon.prototype.model.inventory.attributes.Quality;
import org.dungeon.prototype.model.inventory.attributes.weapon.Handling;
import org.dungeon.prototype.model.inventory.attributes.weapon.Size;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponHandlerMaterial;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponMaterial;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Map;
@Data
@ConfigurationPropertiesScan
@ConfigurationProperties("generation.items.weapon")
public class WeaponGenerationProperties {
    private Map<WeaponType, WeaponDefaultAttributes> defaultAttributes;
    private Map<Handling, AdjustmentAttributes> handlingAdjustmentAttributes;
    private Map<WeaponMaterial, AdjustmentAttributes> weaponMaterialAdjustmentAttributes;
    private Map<WeaponHandlerMaterial, AdjustmentAttributes> weaponHandlerMaterialAdjustmentAttributes;
    private Map<WeaponHandlerMaterial, AdjustmentAttributes> completeMaterialAdjustmentAttributes;
    private Map<Size, SizeAdjustmentAttributes> sizeAdjustmentAttributes;
    private Map<WeaponAttackType, AttackTypeAdjustmentAttributes> attackTypeAdjustmentAttributes;
    private Map<Quality, Double> qualityAdjustmentRatio;
    @Data
    public static class WeaponDefaultAttributes {
        private Integer attack;
        private Double criticalHitChance;
        private Double criticalHitMultiplier;
        private Double chanceToMiss;
        private Double chanceToKnockOut;
    }

    @Data
    public static class AdjustmentAttributes {
        private Double attackRatio;
        private Double chanceToMissRatio;
        private Double criticalChanceRatio;
        private Double criticalMultiplierRatio;
        private Double knockOutChanceRatio;
    }

    @Data
    public static class SizeAdjustmentAttributes {
        private Double attackRatio;
        private Double chanceToMissRatio;
    }

    @Data
    public static class AttackTypeAdjustmentAttributes {
        private Double attackRatio;
        private Double criticalChanceRatio;
        private Double criticalMultiplierRatio;
        private Double knockOutChanceRatio;
    }
}
