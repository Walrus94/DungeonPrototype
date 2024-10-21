package org.dungeon.prototype.service.room.generation.room.content;

import lombok.val;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.dungeon.prototype.model.room.content.Anvil;
import org.dungeon.prototype.model.weight.Weight;
import org.springframework.stereotype.Service;

@Service
public class AnvilFactory {

    /**
     * Generates anvil of weight as close to given as possible
     * @param expectedWeight of resulting anvil
     * @return generated anvil
     */
    public Anvil generateAnvil(Weight expectedWeight) {

        MultivariateFunction objective = point -> objectiveAnvilWeightFunction(
                point,
                expectedWeight
        );

        BOBYQAOptimizer optimizer = new BOBYQAOptimizer(5);

        PointValuePair result = optimizer.optimize(
                new MaxEval(1000),
                new ObjectiveFunction(objective),
                GoalType.MINIMIZE,
                new InitialGuess(new double[]{5.0, 0.001}),
                new SimpleBounds(new double[]{0.0, 0.0},
                        new double[]{Double.POSITIVE_INFINITY, 1.0})
        );

        double[] optimizedAttributes = result.getPoint();

        return Anvil.builder()
                .attackBonus((int) optimizedAttributes[0])
                .chanceToBreakWeapon(optimizedAttributes[1])
                .build();
    }

    private double objectiveAnvilWeightFunction(double[] attributes, Weight expectedWeight) {
        val anvil = Anvil.builder()
                .attackBonus((int) attributes[0])
                .chanceToBreakWeapon(attributes[1])
                .build();

        return expectedWeight.toVector().subtract(anvil.getRoomContentWeight().toVector()).getNorm();
    }
}
