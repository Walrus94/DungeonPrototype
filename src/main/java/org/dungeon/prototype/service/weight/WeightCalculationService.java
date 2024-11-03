package org.dungeon.prototype.service.weight;

import org.dungeon.prototype.model.weight.Weight;
import org.springframework.stereotype.Service;

import static org.dungeon.prototype.util.GenerationUtil.getWeightLimitNormalization;

@Service
public class WeightCalculationService {
    public Weight getExpectedClusterConnectionPointWeight(Weight weight, double limit, int currentStep, int size) {
        return getWeightLimitNormalization(weight, limit, currentStep, size);
    }

    public Weight getExpectedWeigth(Weight weight, double limit, int currentStep, int size) {
       return getWeightLimitNormalization(weight, limit, currentStep, size);
    }

    public Weight getExpectedDeadEndRouteWeight(Weight weight, double limit, int currentStep) {
        return getWeightLimitNormalization(weight, limit, currentStep, currentStep);
    }
}
