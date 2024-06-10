package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.room.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.math3.util.FastMath.max;
import static org.apache.commons.math3.util.FastMath.min;
import static org.dungeon.prototype.model.room.RoomType.NORMAL;

@Slf4j
@UtilityClass
public class RandomUtil {
    private static final RandomDataGenerator random = new RandomDataGenerator();
    //TODO: investigate and configure
    private static final double LAMBDA = 0.1;// Decay rate for special rooms
    private static final double HEALTH_SHRINE_PROBABILITY = 0.99;// Initial probability for healing shrine
    private static final double MANA_SHRINE_PROBABILITY = 0.89;// Initial probability for mana shrine
    private static final double MERCHANT_PROBABILITY = 0.79;// Initial probability for merchant

    public static Direction getRandomDirection(List<Pair<Direction, Double>> pmf) {
        return new EnumeratedDistribution<>(random.getRandomGenerator(), pmf).sample();
    }

    public static Boolean flipAdjustedCoin(Double trueProbability) {
        trueProbability = max(0.0, min(1.0, trueProbability));
        return new EnumeratedDistribution<>(random.getRandomGenerator(), List.of(Pair.create(true, trueProbability), Pair.create(false, 1.0 - trueProbability))).sample();
    }

    public static Integer getRandomInt(int from, int to) {
        return random.nextInt(from, to);
    }

    public static Double getNextUniform(double from, double to) {
        return random.nextUniform(from, to);
    }

    public static Double getNormalDistributionRandomDouble(double mean, double delta) {
        return new NormalDistribution(random.getRandomGenerator(), mean, delta).sample();
    }

    public static RoomType getNextRoomType(Integer roomsLeft,
                                           Integer totalRooms,
                                           Integer roomMonsters,
                                           Integer roomTreasures,
                                           Integer currentStep,
                                           Map<RoomType, Double> defaultProbabilities) {
        log.debug("Generating random room type...");
        List<Pair<RoomType, Double>> probabilities = new ArrayList<>();
        double treasureRatio = 0.0;
        double monsterRatio = 0.0;
        if (!totalRooms.equals(roomsLeft)) {
            treasureRatio = roomTreasures.doubleValue() / (totalRooms.doubleValue() - roomsLeft.doubleValue());
            monsterRatio = roomMonsters.doubleValue() / (totalRooms.doubleValue() - roomsLeft.doubleValue());
        }
        log.debug("Treasure ratio: {}", treasureRatio);
        log.debug("Monster ratio: {}", monsterRatio);

        val specialRoomRatio = new ExponentialDistribution(random.getRandomGenerator(), 1 / LAMBDA).density(currentStep);
        log.debug("Current step: {}, lambda: {}, special room ratio: {}", currentStep, LAMBDA, specialRoomRatio);
        val commonRoomRatio = new NormalDistribution(currentStep.doubleValue() / totalRooms.doubleValue(), 0.1).sample();
        log.debug("Common room ratio: {}", commonRoomRatio);

        val healthShrineProbability = max(0.0, defaultProbabilities.containsKey(RoomType.HEALTH_SHRINE) ?
                defaultProbabilities.get(RoomType.HEALTH_SHRINE) :
                specialRoomRatio * HEALTH_SHRINE_PROBABILITY * (1 - (currentStep - 1) / totalRooms.doubleValue()));
        log.debug("Health shrine probability: {}", healthShrineProbability);
        val manaShrineProbability = max(0.0, defaultProbabilities.containsKey(RoomType.MANA_SHRINE) ?
                defaultProbabilities.get(RoomType.MANA_SHRINE) :
                specialRoomRatio * MANA_SHRINE_PROBABILITY * (1 - (currentStep - 1)/ totalRooms.doubleValue()));
        log.debug("Mana shrine probability: {}", manaShrineProbability);
        val merchantProbability = max(0.0, defaultProbabilities.containsKey(RoomType.MERCHANT) ?
                defaultProbabilities.get(RoomType.MERCHANT) :
                specialRoomRatio * MERCHANT_PROBABILITY * (1 - (currentStep - 1) / totalRooms.doubleValue()));
        log.debug("Merchant probability: {}", merchantProbability);

        val treasureProbability = max(0.0, defaultProbabilities.containsKey(RoomType.TREASURE) ?
                defaultProbabilities.get(RoomType.TREASURE) :
                (1 - treasureRatio) * commonRoomRatio);
        log.debug("Treasure probability: {}", treasureProbability);
        val monsterProbability = max(0.0, defaultProbabilities.containsKey(RoomType.MONSTER) ?
                defaultProbabilities.get(RoomType.MONSTER) :
                (1 - monsterRatio) * commonRoomRatio);
        log.debug("Monster probability: {}", merchantProbability);
        val normalProbability = max(0.0, 1.0 - monsterProbability - treasureProbability - healthShrineProbability - manaShrineProbability - merchantProbability);
        log.debug("Normal room probability: {}", normalProbability);

        probabilities.add(Pair.create(RoomType.MONSTER, monsterProbability));
        probabilities.add(Pair.create(RoomType.TREASURE, treasureProbability));
        probabilities.add(Pair.create(RoomType.HEALTH_SHRINE, healthShrineProbability));
        probabilities.add(Pair.create(RoomType.MANA_SHRINE, manaShrineProbability));
        probabilities.add(Pair.create(RoomType.MERCHANT, merchantProbability));
        probabilities.add(Pair.create(RoomType.NORMAL, normalProbability));
        if (probabilities.stream().mapToDouble(Pair::getValue).sum() == 0.0) {
            return NORMAL;
        }
        return getRoomTypeDistribution(probabilities).sample();
    }

    private static EnumeratedDistribution<RoomType> getRoomTypeDistribution(List<Pair<RoomType, Double>> probabilities) {
        return new EnumeratedDistribution<>(normalizeProbabilities(probabilities));
    }

    private static List<Pair<RoomType,Double>> normalizeProbabilities(List<Pair<RoomType, Double>> probabilities) {
        val sum = probabilities.stream().mapToDouble(Pair::getValue).sum();
        return sum == 1 ? probabilities :
                probabilities.stream().map(pair -> new Pair<>(pair.getKey(), pair.getValue() / sum)).collect(Collectors.toList());
    }
}
