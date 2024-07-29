package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.service.room.generation.LevelRoomTypeClusters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.apache.commons.math3.util.FastMath.PI;
import static org.apache.commons.math3.util.FastMath.cos;
import static org.apache.commons.math3.util.FastMath.exp;
import static org.apache.commons.math3.util.FastMath.max;
import static org.apache.commons.math3.util.FastMath.min;
import static org.dungeon.prototype.model.room.RoomType.NORMAL;

@Slf4j
@UtilityClass
public class RandomUtil {
    private static final RandomDataGenerator random = new RandomDataGenerator();
    public static  <T extends EnumAttribute> T getRandomEnumValue(List<T> values) {
        return new EnumeratedDistribution<>(random.getRandomGenerator(), values.stream().map(t -> new Pair<>(t, 1.0)).toList()).sample();
    }

    public static  <T extends EnumAttribute> T getRandomEnumValue(List<T> values, List<T> exclusions) {
        return new EnumeratedDistribution<>(random.getRandomGenerator(), values.stream().filter(t -> !exclusions.contains(t)).map(t -> new Pair<>(t, 1.0)).toList()).sample();
    }

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

    public static Double getRandomUniform(double from, double to) {
        return random.nextUniform(from, to);
    }

    public static Double getNormalDistributionRandomDouble(double mean, double sd) {
        return new NormalDistribution(random.getRandomGenerator(), mean, sd).sample();
    }

    public static RoomType getNextRoomType(LevelRoomTypeClusters levelRoomTypeClusters,
                                           Integer currentStep,
                                           Map<RoomType, Double> defaultProbabilities) {
        val roomsLeft = levelRoomTypeClusters.getRoomsLeft();
        val totalRooms = levelRoomTypeClusters.getTotalRooms();
        val roomTreasures = levelRoomTypeClusters.getRoomTreasures();
        val roomMonster = levelRoomTypeClusters.getMonsterClassesCounters();
        val healthShrineInitialProbability = levelRoomTypeClusters.getHealthShrineInitialProbability();
        val manaShrineInitialProbability = levelRoomTypeClusters.getManaShrineInitialProbability();
        val merchantInitialProbability = levelRoomTypeClusters.getMerchantInitialProbability();
        log.debug("Total rooms: {}, rooms left: {}, treasure rooms: {}, monster rooms: {}", totalRooms, roomsLeft, roomTreasures, roomMonster);
        log.debug("Initial probabilities: Health Shrine: {}, Mana Shrine: {}, Merchant: {}",
                healthShrineInitialProbability, manaShrineInitialProbability, merchantInitialProbability);
        log.debug("Generating random room type...");
        double treasureRatio = totalRooms.equals(roomsLeft) ? 0.0 : roomTreasures.doubleValue() / (totalRooms.doubleValue() - roomsLeft.doubleValue());
        double werewolfRatio = totalRooms.equals(roomsLeft) ? 0.0 : roomMonster.get(MonsterClass.WEREWOLF).doubleValue() / (totalRooms.doubleValue() - roomsLeft.doubleValue());
        double swampBeastRatio = totalRooms.equals(roomsLeft) ? 0.0 : roomMonster.get(MonsterClass.SWAMP_BEAST).doubleValue() / (totalRooms.doubleValue() - roomsLeft.doubleValue());
        double vampireRatio = totalRooms.equals(roomsLeft) ? 0.0 : roomMonster.get(MonsterClass.VAMPIRE).doubleValue() / (totalRooms.doubleValue() - roomsLeft.doubleValue());
        double dragonRatio = totalRooms.equals(roomsLeft) ? 0.0 : roomMonster.get(MonsterClass.DRAGON).doubleValue() / (totalRooms.doubleValue() - roomsLeft.doubleValue());
        double zombieRatio = totalRooms.equals(roomsLeft) ? 0.0 : roomMonster.get(MonsterClass.ZOMBIE).doubleValue() / (totalRooms.doubleValue() - roomsLeft.doubleValue());
        log.debug("Treasure ratio: {}", treasureRatio);
        log.debug("Monsters ratio: Werewolf - {}, Swamp beast - {}, Vampire - {}, Dragon - {}, Zombie - {}",
                werewolfRatio, swampBeastRatio, vampireRatio, dragonRatio, zombieRatio);

        val specialRoomDistribution = exp(1 - currentStep);
        log.debug("Current step: {}, special room distribution: {}", currentStep, specialRoomDistribution);
        val treasureRoomDistribution = (cos(PI * (currentStep - 1) / 2) + 1) / 2;
        log.debug("Current treasure room distribution: {}", treasureRoomDistribution);
        val monsterRoomDistribution = (1 - cos(PI * (currentStep - 1))) / 2;
        log.debug("Current monster room distribution: {}", monsterRoomDistribution);
        val normalRoomDistribution = (currentStep.doubleValue() - 1.0) / (totalRooms.doubleValue() * totalRooms.doubleValue());
        log.debug("Current normal room distribution: {}", normalRoomDistribution);

        val healthShrineProbability = max(0.0, defaultProbabilities.containsKey(RoomType.HEALTH_SHRINE) ?
                defaultProbabilities.get(RoomType.HEALTH_SHRINE) :
                specialRoomDistribution * healthShrineInitialProbability);
        log.debug("Health shrine probability: {}", healthShrineProbability);
        val manaShrineProbability = max(0.0, defaultProbabilities.containsKey(RoomType.MANA_SHRINE) ?
                defaultProbabilities.get(RoomType.MANA_SHRINE) :
                specialRoomDistribution * manaShrineInitialProbability);
        log.debug("Mana shrine probability: {}", manaShrineProbability);
        val merchantProbability = max(0.0, defaultProbabilities.containsKey(RoomType.MERCHANT) ?
                defaultProbabilities.get(RoomType.MERCHANT) :
                specialRoomDistribution * merchantInitialProbability);
        log.debug("Merchant probability: {}", merchantProbability);

        val treasureProbability = max(0.0, defaultProbabilities.containsKey(RoomType.TREASURE) ?
                defaultProbabilities.get(RoomType.TREASURE) :
                (1 - treasureRatio) * treasureRoomDistribution);
        log.debug("Treasure probability: {}", treasureProbability);
        val werewolfProbability = max(0.0, defaultProbabilities.containsKey(RoomType.WEREWOLF) ?
                defaultProbabilities.get(RoomType.WEREWOLF) :
                (1 - werewolfRatio) * monsterRoomDistribution);
        log.debug("Werewolf probability: {}", werewolfProbability);
        val swampBeastProbability = max(0.0, defaultProbabilities.containsKey(RoomType.SWAMP_BEAST) ?
                defaultProbabilities.get(RoomType.SWAMP_BEAST) :
                (1 - swampBeastRatio) * monsterRoomDistribution);
        log.debug("Swamp beast probability: {}", swampBeastProbability);
        val vampireProbability = max(0.0, defaultProbabilities.containsKey(RoomType.VAMPIRE) ?
                defaultProbabilities.get(RoomType.VAMPIRE) :
                (1 - vampireRatio) * monsterRoomDistribution);
        log.debug("Vampire probability: {}", vampireProbability);
        val dragonProbability = max(0.0, defaultProbabilities.containsKey(RoomType.DRAGON) ?
                defaultProbabilities.get(RoomType.DRAGON) :
                (1 - dragonRatio) * monsterRoomDistribution);
        log.debug("Dragon probability: {}", dragonProbability);
        val zombieProbability = max(0.0, defaultProbabilities.containsKey(RoomType.ZOMBIE) ?
                defaultProbabilities.get(RoomType.ZOMBIE) :
                (1 - zombieRatio) * monsterRoomDistribution);
        log.debug("Zombie probability: {}", zombieProbability);
        val normalProbability = max(0.0, normalRoomDistribution);
        log.debug("Normal room probability: {}", normalProbability);

        List<Pair<RoomType, Double>> probabilities = new ArrayList<>();
        probabilities.add(Pair.create(RoomType.WEREWOLF, werewolfProbability));
        probabilities.add(Pair.create(RoomType.SWAMP_BEAST, swampBeastProbability));
        probabilities.add(Pair.create(RoomType.VAMPIRE, vampireProbability));
        probabilities.add(Pair.create(RoomType.DRAGON, dragonProbability));
        probabilities.add(Pair.create(RoomType.ZOMBIE, zombieProbability));
        probabilities.add(Pair.create(RoomType.TREASURE, treasureProbability));
        probabilities.add(Pair.create(RoomType.HEALTH_SHRINE, healthShrineProbability));
        probabilities.add(Pair.create(RoomType.MANA_SHRINE, manaShrineProbability));
        probabilities.add(Pair.create(RoomType.MERCHANT, merchantProbability));
        probabilities.add(Pair.create(RoomType.NORMAL, normalProbability));
        if (probabilities.stream().mapToDouble(Pair::getValue).sum() == 0.0) {
            return NORMAL;
        }
        return getEnumeratedDistribution(probabilities).sample();
    }

    public static Stream<Weapon> getRandomWeaponsStream(List<Pair<Weapon, Double>> probabilities, Integer weaponPerGame) {
        return Arrays.stream(getEnumeratedDistribution(probabilities).sample(weaponPerGame))
                .map(value -> (Weapon) value);
    }

    public static Integer getRandomEffectAddition() {
        return getEnumeratedDistribution(List.of(
                Pair.create(1, 0.1),
                Pair.create(2, 0.3),
                Pair.create(3, 0.5),
                Pair.create(5, 0.3),
                Pair.create(7, 0.1)
        )).sample();
    }

    public static Double getRandomEffectMultiplier() {
        return getEnumeratedDistribution(List.of(
                Pair.create(1.1, 0.1),
                Pair.create(1.2, 0.2),
                Pair.create(1.3, 0.5),
                Pair.create(1.5, 0.2),
                Pair.create(1.7, 0.1)
        )).sample();
    }

    private static <T>EnumeratedDistribution<T> getEnumeratedDistribution(List<Pair<T, Double>> probabilities) {
        return new EnumeratedDistribution<>(probabilities);
    }
}
