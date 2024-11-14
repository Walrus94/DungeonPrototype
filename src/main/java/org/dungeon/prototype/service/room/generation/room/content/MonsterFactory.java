package org.dungeon.prototype.service.room.generation.room.content;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.dungeon.prototype.model.effect.ExpirableAdditionEffect;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.monster.MonsterAttack;
import org.dungeon.prototype.model.monster.MonsterAttackType;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.properties.GenerationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.Double.POSITIVE_INFINITY;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.HEALTH;
import static org.dungeon.prototype.util.RandomUtil.getMagicTypeByMonsterClass;
import static org.dungeon.prototype.util.RoomGenerationUtils.calculateMonsterWeight;

@Slf4j
@Service
public class MonsterFactory {
    @Autowired
    private GenerationProperties generationProperties;

    /**
     * Generates monster of requested class with weight as close to given as possible
     * @param weight expected weight
     * @param monsterClass requested monster class
     * @return generated monster
     */
    public Monster generateMonsterByExpectedWeight(Weight weight, MonsterClass monsterClass) {
        val properties = generationProperties.getMonsters().get(monsterClass);
        val primaryAttack = properties.getPrimaryAttackType();
        val secondaryAttack = properties.getSecondaryAttackType();

        val expectedHealthPlusAttack = weight.getHp() + weight.getArmor()  *
                (weight.getChanceToKnockout() + weight.getCriticalHitChance() + weight.getAttack());

        // Define optimization objective
        MultivariateFunction objective = point -> objectiveMonsterWeightFunction(
                point,
                weight,
                primaryAttack,
                secondaryAttack,
                expectedHealthPlusAttack);

        CMAESOptimizer optimizer = new CMAESOptimizer(
                10000,
                1e-9,
                true,
                0,
                10,
                new RandomDataGenerator().getRandomGenerator(),
                false,
                null);
        val lowerBound = new double[]{
                1.0,
                1.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        };
        val initialGuess = new double[]{
                5.0,
                5.0,
                0.5,
                1.0,
                0.5,
                0.5,
                0.5,
                1.0,
                1.0,
                1.0,
                0.5,
                1.0,
                0.5,
                0.5,
                0.5,
                1.0,
                1.0
        };
        val upperBound = new double[]{
                POSITIVE_INFINITY,
                POSITIVE_INFINITY,
                1.0,
                POSITIVE_INFINITY,
                1.0,
                1.0,
                1.0,
                POSITIVE_INFINITY,
                POSITIVE_INFINITY,
                POSITIVE_INFINITY,
                1.0,
                POSITIVE_INFINITY,
                1.0,
                1.0,
                1.0,
                POSITIVE_INFINITY,
                POSITIVE_INFINITY
        };
        double[] sigma = new double[17];
        // Can be tuned based on the scale of the function
        Arrays.fill(sigma, 0.005);

        // Optimization problem setup
        PointValuePair result = optimizer.optimize(
                new MaxEval(10000),                             // Maximum evaluations
                new ObjectiveFunction(objective),              // Objective function
                GoalType.MINIMIZE,                             // Minimization goal
                new InitialGuess(initialGuess),
                new CMAESOptimizer.Sigma(sigma),
                new CMAESOptimizer.PopulationSize(12),
                new SimpleBounds(lowerBound, upperBound)
        );

        // Optimized attributes result
        double[] optimizedAttributes = result.getPoint();

        return Monster.builder()
                .monsterClass(monsterClass)
                .maxHp((int) optimizedAttributes[0])
                .hp((int) optimizedAttributes[0])
                .primaryAttack(MonsterAttack.builder()
                        .attackType(primaryAttack)
                        .attack((int) optimizedAttributes[1])
                        .criticalHitChance(optimizedAttributes[2])
                        .criticalHitMultiplier(optimizedAttributes[3])
                        .chanceToKnockOut(optimizedAttributes[4])
                        .chanceToMiss(optimizedAttributes[5])
                        .causingEffectProbability(optimizedAttributes[6])
                        .effect(ExpirableAdditionEffect.builder()
                                .isAccumulated(true)
                                .attribute(HEALTH)
                                .amount((int) optimizedAttributes[7])
                                .turnsLeft((int) optimizedAttributes[8])
                                .build())
                        .build())
                .secondaryAttack(MonsterAttack.builder()
                        .attackType(secondaryAttack)
                        .attack((int) optimizedAttributes[9])
                        .criticalHitChance(optimizedAttributes[10])
                        .criticalHitMultiplier(optimizedAttributes[11])
                        .chanceToKnockOut(optimizedAttributes[12])
                        .chanceToMiss(optimizedAttributes[13])
                        .causingEffectProbability(optimizedAttributes[14])
                        .effect(ExpirableAdditionEffect.builder()
                                .isAccumulated(true)
                                .attribute(HEALTH)
                                .amount((int) optimizedAttributes[15])
                                .turnsLeft((int) optimizedAttributes[16])
                                .build())
                        .build())
                .magicType(getMagicTypeByMonsterClass(monsterClass))
                .effects(new ArrayList<>())
                .build();
    }

    // Objective function to minimize (absolute difference between target and current weight)
    private static double objectiveMonsterWeightFunction(double[] attributes,
                                                         Weight targetWeight, MonsterAttackType primaryAttack, MonsterAttackType secondaryAttack, double expectedHealthXAttack) {
        val currentWeight = calculateMonsterWeight((int) attributes[0], (int) attributes[0],
                MonsterAttack.builder()
                        .attackType(primaryAttack)
                        .attack((int) attributes[1])
                        .criticalHitChance(attributes[2])
                        .criticalHitMultiplier(attributes[3])
                        .chanceToKnockOut(attributes[4])
                        .chanceToMiss(attributes[5])
                        .causingEffectProbability(attributes[6])
                        .effect(ExpirableAdditionEffect.builder()
                                .isAccumulated(true)
                                .attribute(HEALTH)
                                .amount((int) attributes[7])
                                .turnsLeft((int) attributes[8])
                                .build())
                        .build(),
                MonsterAttack.builder()
                        .attackType(secondaryAttack)
                        .attack((int) attributes[9])
                        .criticalHitChance(attributes[10])
                        .criticalHitMultiplier(attributes[11])
                        .chanceToKnockOut(attributes[12])
                        .chanceToMiss(attributes[13])
                        .causingEffectProbability(attributes[14])
                        .effect(ExpirableAdditionEffect.builder()
                                .isAccumulated(true)
                                .attribute(HEALTH)
                                .amount((int) attributes[15])
                                .turnsLeft((int) attributes[16])
                                .build())
                        .build());
        return targetWeight.add(currentWeight.getNegative()).toVector().getNorm();
    }
}
