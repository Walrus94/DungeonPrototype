package org.dungeon.prototype.service.effect;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.ExpirableAdditionEffect;
import org.dungeon.prototype.model.effect.PermanentAdditionEffect;
import org.dungeon.prototype.model.effect.PermanentMultiplicationEffect;
import org.dungeon.prototype.model.effect.attributes.Action;
import org.dungeon.prototype.model.effect.attributes.EffectApplicant;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.model.inventory.Item;
import org.springframework.stereotype.Service;

import static org.dungeon.prototype.model.effect.attributes.Action.MULTIPLY;

@Slf4j
@Service
public class EffectFactory {

    /**
     * Generates given attribute regeneration effect
     *
     * @param attribute         to regenerate
     * @param expectedWeightAbs resulting effect expected weight norm
     * @return expirable accumulated addition effect
     */
    public ExpirableAdditionEffect generateRegenerationEffect(EffectAttribute attribute, double expectedWeightAbs) {
        MultivariateFunction objective = point -> objectiveEffectGenerationFunction(
                point,
                attribute,
                expectedWeightAbs
        );

        BOBYQAOptimizer optimizer = new BOBYQAOptimizer(4);

        PointValuePair result = optimizer.optimize(
                new MaxEval(10000),                             // Maximum evaluations
                new ObjectiveFunction(objective),              // Objective function
                GoalType.MINIMIZE,                             // Minimization goal
                new InitialGuess(new double[]{1.0, 3.0}),
                new SimpleBounds(new double[]{0.0, 0.0},
                        new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY})
        );

        double[] optimizedAttributes = result.getPoint();

        return ExpirableAdditionEffect.builder()
                .isAccumulated(true)
                .applicableTo(EffectApplicant.PLAYER)
                .attribute(attribute)
                .amount((int) optimizedAttributes[0])
                .turnsLeft((int) optimizedAttributes[1])
                .build();
    }

    /**
     * Generates item effect to change its weight by expected value
     *
     * @param item                 to add effect to
     * @param attribute            attribute to apply
     * @param action               {@link Action#ADD} or {@link Action#MULTIPLY}
     * @param expectedWeightChange expected weight norm delta
     * @return generated effect
     */
    public Effect generateItemEffect(Item item, EffectAttribute attribute, Action action, double expectedWeightChange) {
        log.info("Generating item effect for item {} with expected weight change {}", item.getId(), expectedWeightChange);
        UnivariateFunction objective = point -> objectiveEffectWeightChangeFunction(
                point,
                item,
                attribute,
                action,
                expectedWeightChange);

        // Powell's method for multidimensional optimization)
        BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);

        // Optimization problem setup
        UnivariatePointValuePair result = optimizer.optimize(
                new MaxEval(1000),                             // Maximum evaluations
                new UnivariateObjectiveFunction(objective),              // Objective function
                GoalType.MINIMIZE,                             // Minimization goal
                new InitialGuess(new double[]{1.0})
        );

        // Optimized attributes result
        double optimizedAttribute = result.getPoint();

        return MULTIPLY.equals(action) ?
                PermanentMultiplicationEffect
                        .builder()
                        .applicableTo(EffectApplicant.ITEM)
                        .attribute(attribute)
                        .multiplier(optimizedAttribute)
                        .build() :
                PermanentAdditionEffect
                        .builder()
                        .applicableTo(EffectApplicant.ITEM)
                        .attribute(attribute)
                        .amount((int) optimizedAttribute)
                        .build();
    }

    private static double objectiveEffectWeightChangeFunction(double point, Item item, EffectAttribute attribute, Action action, double expectedWeightChange) {
        log.info("Calculating objective function for item {} with expected weight change {}", item.getId(), expectedWeightChange);
        val oldWeight = item.getWeight().toVector();
        switch (action) {
            case ADD -> item.getEffects().add(
                    PermanentAdditionEffect.builder()
                            .applicableTo(EffectApplicant.ITEM)
                            .attribute(attribute)
                            .amount((int) point)
                            .build()
            );
            case MULTIPLY -> item.getEffects().add(
                    PermanentMultiplicationEffect.builder()
                            .applicableTo(EffectApplicant.ITEM)
                            .attribute(attribute)
                            .multiplier(point)
                            .build()
            );
            case OTHER -> {
            }
        }
        return Math.abs(expectedWeightChange - item.getWeight().toVector().subtract(oldWeight).getNorm());
    }

    private static double objectiveEffectGenerationFunction(double[] point, EffectAttribute attribute, double expectedWeightAbs) {
        ExpirableAdditionEffect effect = ExpirableAdditionEffect.builder()
                .attribute(attribute)
                .applicableTo(EffectApplicant.PLAYER)
                .isAccumulated(true)
                .amount((int) point[0])
                .turnsLeft((int) point[1])
                .build();
        return Math.abs(expectedWeightAbs - effect.getWeight().toVector().getNorm());
    }
}
