package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.ExpirableAdditionEffect;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.model.monster.MonsterAttack;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.weight.Weight;

import java.util.List;

@Slf4j
@UtilityClass
public class RoomGenerationUtils {

    public static RoomType convertToRoomType(MonsterClass monsterClass) {
        return switch (monsterClass) {
            case WEREWOLF -> RoomType.WEREWOLF;
            case SWAMP_BEAST -> RoomType.SWAMP_BEAST;
            case VAMPIRE -> RoomType.VAMPIRE;
            case DRAGON -> RoomType.DRAGON;
            case ZOMBIE -> RoomType.ZOMBIE;
        };
    }

    public static MonsterClass convertToMonsterClass(RoomType roomType) {
        return switch (roomType) {
            case ZOMBIE -> MonsterClass.ZOMBIE;
            case WEREWOLF -> MonsterClass.WEREWOLF;
            case VAMPIRE -> MonsterClass.VAMPIRE;
            case SWAMP_BEAST -> MonsterClass.SWAMP_BEAST;
            case DRAGON -> MonsterClass.DRAGON;
            default -> throw new IllegalStateException("Unexpected value: " + roomType);
        };
    }

    public static List<RoomType> getMonsterRoomTypes() {
        return List.of(RoomType.WEREWOLF, RoomType.VAMPIRE, RoomType.SWAMP_BEAST, RoomType.ZOMBIE, RoomType.DRAGON);
    }

    public static Weight calculateMonsterWeight(
            Integer hp, Integer maxHp, MonsterAttack primaryAttack,
            MonsterAttack secondaryAttack) {
        return Weight.builder()
                .hpToMaxHp((double) ((hp / maxHp) * hp))
                .hpDeficiencyToMaxHp((double) (((maxHp - hp) / maxHp) * hp))
                .criticalHitChance(primaryAttack.getCriticalHitChance() * primaryAttack.getCriticalHitMultiplier() * primaryAttack.getAttack() +
                        secondaryAttack.getCriticalHitChance() * secondaryAttack.getCriticalHitMultiplier())
                .chanceToKnockout(primaryAttack.getChanceToKnockOut() * secondaryAttack.getChanceToKnockOut())
                .attack((1.0 - primaryAttack.getChanceToMiss()) * (1.0 - secondaryAttack.getChanceToMiss()))
                .build();
    }

    public static ExpirableAdditionEffect getFireSpitEffect(Integer amount, Integer turns) {
        return ExpirableAdditionEffect.builder()
                .attribute(EffectAttribute.HEALTH)
                .amount(amount)
                .turnsLeft(turns)
                .isAccumulated(true)
                .build();
    }

    public static Effect getGrowlEffect(Integer amount, Integer turns) {
        return ExpirableAdditionEffect.builder()
                .attribute(EffectAttribute.HEALTH)
                .amount(amount)
                .turnsLeft(turns)
                .isAccumulated(true)
                .build();
    }

    public static Effect getBleedingEffect(Integer amount, Integer turns) {
        return ExpirableAdditionEffect.builder()
                .attribute(EffectAttribute.HEALTH)
                .amount(amount)
                .turnsLeft(turns)
                .isAccumulated(true)
                .build();
    }

    public static Effect getPoisonEffect(Integer amount, Integer turns) {
        return ExpirableAdditionEffect.builder()
                .attribute(EffectAttribute.HEALTH)
                .amount(amount)
                .turnsLeft(turns)
                .isAccumulated(true)
                .build();
    }

    public static Effect getBiteEffect(Integer effectAmount, Integer turns) {
        return null;
    }

    public static Effect getVampireEffect(Integer amount, Integer turns) {
        return ExpirableAdditionEffect.builder()
                .attribute(EffectAttribute.HEALTH)
                .amount(amount)
                .turnsLeft(turns)
                .isAccumulated(true)
                .build();
    }
}
