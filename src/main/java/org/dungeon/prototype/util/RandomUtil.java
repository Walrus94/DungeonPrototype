package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.model.inventory.attributes.EnumAttribute;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.weight.Weight;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.math3.util.FastMath.PI;
import static org.apache.commons.math3.util.FastMath.cos;
import static org.apache.commons.math3.util.FastMath.exp;
import static org.apache.commons.math3.util.FastMath.max;
import static org.apache.commons.math3.util.FastMath.min;
import static org.apache.commons.math3.util.FastMath.sin;
import static org.apache.commons.math3.util.FastMath.sqrt;
import static org.dungeon.prototype.model.room.RoomType.ANVIL;
import static org.dungeon.prototype.model.room.RoomType.DRAGON;
import static org.dungeon.prototype.model.room.RoomType.NORMAL;
import static org.dungeon.prototype.model.room.RoomType.SWAMP_BEAST;
import static org.dungeon.prototype.model.room.RoomType.TREASURE;
import static org.dungeon.prototype.model.room.RoomType.VAMPIRE;
import static org.dungeon.prototype.model.room.RoomType.WEREWOLF;
import static org.dungeon.prototype.model.room.RoomType.ZOMBIE;
import static org.dungeon.prototype.model.weight.Sign.NEGATIVE;
import static org.dungeon.prototype.model.weight.Sign.POSITIVE;
import static org.dungeon.prototype.model.weight.Sign.ZERO;
import static org.dungeon.prototype.util.RoomGenerationUtils.convertToMonsterClass;

@Slf4j
@UtilityClass
public class RandomUtil {
    private static final RandomDataGenerator random = new RandomDataGenerator();

    public static <T extends EnumAttribute> T getRandomWeightedEnumValue(Map<T, Double> values) {
        return new EnumeratedDistribution<>(random.getRandomGenerator(), values.entrySet().stream().map(entry -> new Pair<>(entry.getKey(), entry.getValue())).toList()).sample();
    }

    public static Boolean flipAdjustedCoin(Double trueProbability) {
        trueProbability = max(0.0, min(1.0, trueProbability));
        return new EnumeratedDistribution<>(random.getRandomGenerator(), List.of(Pair.create(true, trueProbability), Pair.create(false, 1.0 - trueProbability))).sample();
    }

    public static Integer getRandomInt(int from, int to) {
        return random.nextInt(from, to);
    }

    public static Double getNormalDistributionRandomDouble(double mean, double sd) {
        return new NormalDistribution(random.getRandomGenerator(), mean, sd).sample();
    }

    public static MagicType getRandomMagicType() {
        double angle = 2 * PI * random.nextUniform(0.0, 1.0);

        val divine = sin(angle);
        val arcane = cos(angle);

        return MagicType.of(divine, arcane);
    }

    /**
     * Generates random room type
     * @param expectedWeight input weight
     * @param currentStep current progress across overall level generation
     * @param totalRooms rooms on level
     * @return next room type
     */
    public static RoomType getRandomRoomType(Weight expectedWeight,
                                             Integer currentStep,
                                             Integer totalRooms) {
        log.debug("Current step: {} / {}", currentStep, totalRooms);
        Double positiveRoomDistribution = getPeriodicBinaryDistributionStartingFromOne(currentStep);
        log.debug("Positive weight treasure room distribution: {}", positiveRoomDistribution);
        Double negativeRoomDistribution = getPeriodicBinaryDistributionStartingFromZero(currentStep);
        log.debug("Negative weight room distribution: {}", negativeRoomDistribution);
        Double normalRoomDistribution = (double) (currentStep / totalRooms);
        log.debug("Current normal room distribution: {}", normalRoomDistribution);

        return switch (getEnumeratedDistribution(List.of(Pair.create(NEGATIVE, negativeRoomDistribution),
                Pair.create(ZERO, normalRoomDistribution), Pair.create(POSITIVE, positiveRoomDistribution))).sample()) {
            case ZERO -> NORMAL;
            case POSITIVE -> {
                List<Pair<RoomType, Double>> probabilities = new ArrayList<>();
                val hpToMaxHp = expectedWeight.getMaxHp() == 0 ? expectedWeight.getHp() :
                        expectedWeight.getHp() / expectedWeight.getMaxHp();
                val hpDeficiencyToMaxHp = expectedWeight.getMaxHp() == 0 ? expectedWeight.getHp() :
                        (expectedWeight.getMaxHp() - expectedWeight.getHp()) / expectedWeight.getMaxHp();
                val manaToMaxMana = expectedWeight.getMaxMana() == 0 ? expectedWeight.getMana() :
                        expectedWeight.getMana() / expectedWeight.getMaxMana();
                val manaDeficiencyToMaxMana = expectedWeight.getMaxMana() == 0 ? expectedWeight.getMana() :
                        (expectedWeight.getMaxMana() - expectedWeight.getMana()) / expectedWeight.getMaxMana();
                val armorToMaxArmor = expectedWeight.getMaxArmor() == 0 ? expectedWeight.getArmor() :
                        expectedWeight.getArmor() / expectedWeight.getMaxArmor();
                val goldBonus = expectedWeight.getGoldBonus();
                val specialRoomDistribution = getExponentialDistribution(currentStep, 0.0);
                log.debug("Special room distribution: {}", specialRoomDistribution);
                val treasureRoomDistribution =  sqrt(1.0 - (double) (currentStep / totalRooms));
                log.debug("Treasure room distribution: {}", treasureRoomDistribution);
                val healthShrineDistribution =
                        getExponentialDistribution(currentStep, totalRooms * hpDeficiencyToMaxHp);
                log.debug("Health shrine distribution: {}", healthShrineDistribution);
                val manaShrineDistribution =
                        getExponentialDistribution(currentStep, totalRooms * manaDeficiencyToMaxMana);
                log.debug("Mana shrine distribution: {}", manaShrineDistribution);

                val healthShrineProbability = healthShrineDistribution * hpToMaxHp;
                log.debug("Health shrine probability: {}", healthShrineProbability);
                val manaShrineProbability = manaShrineDistribution * manaToMaxMana;
                log.debug("Mana shrine probability: {}", manaShrineProbability);
                val merchantProbability = specialRoomDistribution * goldBonus; //TODO: add potential (unequipped items) weight
                log.debug("Merchant probability: {}", merchantProbability);
                val anvilProbability = specialRoomDistribution * armorToMaxArmor;
                val treasureProbability = treasureRoomDistribution * goldBonus; //TODO: add potential (unequipped items) weight
                log.debug("Treasure probability: {}", treasureProbability);

                val sumProb = healthShrineProbability + manaShrineProbability + merchantProbability + treasureProbability + anvilProbability;

                if (sumProb == 0.0) {
                    yield  NORMAL;
                }

                val positiveProbabilitiesNormalizingFactor =
                        1 / sumProb;

                probabilities.add(Pair.create(RoomType.TREASURE, max(0.0, min(1.0, treasureProbability * positiveProbabilitiesNormalizingFactor))));
                probabilities.add(Pair.create(RoomType.HEALTH_SHRINE, max(0.0, min(1.0, healthShrineProbability * positiveProbabilitiesNormalizingFactor))));
                probabilities.add(Pair.create(RoomType.MANA_SHRINE, max(0.0, min(1.0, manaShrineProbability * positiveProbabilitiesNormalizingFactor))));
                probabilities.add(Pair.create(RoomType.MERCHANT, max(0.0, min(1.0, merchantProbability * positiveProbabilitiesNormalizingFactor))));
                probabilities.add(Pair.create(ANVIL, max(0.0, min(1.0, anvilProbability * positiveProbabilitiesNormalizingFactor))));

                yield getEnumeratedDistribution(probabilities).sample();
            }
            case NEGATIVE -> {
                List<Pair<RoomType, Double>> probabilities = new ArrayList<>();

                val arcaneMagic = expectedWeight.getArcaneMagic();
                val divineMagic = expectedWeight.getDivineMagic();

                if (arcaneMagic == 0 && divineMagic == 0) {
                    yield getEnumeratedDistribution(List.of(
                            Pair.create(WEREWOLF, 0.2),
                            Pair.create(DRAGON, 0.2),
                            Pair.create(VAMPIRE, 0.2),
                            Pair.create(ZOMBIE, 0.2),
                            Pair.create(SWAMP_BEAST, 0.2))).sample();
                }

                val expectedMagicWeightVector = MagicType.of(divineMagic, arcaneMagic);

                val werewolfMagicDiffAbs = expectedMagicWeightVector.toVector()
                        .subtract(getMagicByMonsterType(WEREWOLF).toVector()).getNorm();
                val swampBeastMagicDiffAbs = expectedMagicWeightVector.toVector()
                        .subtract(getMagicByMonsterType(SWAMP_BEAST).toVector()).getNorm();
                val vampireMagicDiffAbs = expectedMagicWeightVector.toVector()
                        .subtract(getMagicByMonsterType(VAMPIRE).toVector()).getNorm();
                val dragonMagicDiffAbs = expectedMagicWeightVector.toVector()
                        .subtract(getMagicByMonsterType(DRAGON).toVector()).getNorm();
                val zombieMagicDiffAbs = expectedMagicWeightVector.toVector()
                        .subtract(getMagicByMonsterType(ZOMBIE).toVector()).getNorm();

                val werewolfRawProbability = werewolfMagicDiffAbs == 0.0 ? 1.0 :  1 / werewolfMagicDiffAbs;
                val swampBeastRawProbability = swampBeastMagicDiffAbs == 0.0 ? 1.0 : 1 / swampBeastMagicDiffAbs;
                val vampireRawProbability = vampireMagicDiffAbs == 0.0 ? 1.0 : 1 / vampireMagicDiffAbs;
                val dragonRawProbability = dragonMagicDiffAbs == 0.0 ? 1.0 : 1/ dragonMagicDiffAbs;
                val zombieRawProbability = zombieMagicDiffAbs == 0.0 ? 1.0 : 1 / zombieMagicDiffAbs;

                val sumProb = werewolfRawProbability + swampBeastRawProbability +
                        vampireRawProbability + dragonRawProbability + zombieRawProbability;

                if (sumProb == 0.0) {
                    yield getEnumeratedDistribution(List.of(
                            Pair.create(WEREWOLF, 0.2),
                            Pair.create(DRAGON, 0.2),
                            Pair.create(VAMPIRE, 0.2),
                            Pair.create(ZOMBIE, 0.2),
                            Pair.create(SWAMP_BEAST, 0.2))).sample();
                }

                val monsterNormalizingFactor = 1 / (sumProb);

                val werewolfProbability = werewolfRawProbability * monsterNormalizingFactor;
                log.debug("Werewolf probability: {}", werewolfProbability);
                val swampBeastProbability =  swampBeastRawProbability * monsterNormalizingFactor;
                log.debug("Swamp beast probability: {}", swampBeastProbability);
                val vampireProbability =  vampireRawProbability * monsterNormalizingFactor;
                log.debug("Vampire probability: {}", vampireProbability);
                val dragonProbability =  dragonRawProbability * monsterNormalizingFactor;
                log.debug("Dragon probability: {}", dragonProbability);
                val zombieProbability =  zombieRawProbability * monsterNormalizingFactor;
                log.debug("Zombie probability: {}", zombieProbability);

                probabilities.add(Pair.create(RoomType.WEREWOLF, werewolfProbability));
                probabilities.add(Pair.create(RoomType.SWAMP_BEAST, swampBeastProbability));
                probabilities.add(Pair.create(RoomType.VAMPIRE, vampireProbability));
                probabilities.add(Pair.create(RoomType.DRAGON, dragonProbability));
                probabilities.add(Pair.create(RoomType.ZOMBIE, zombieProbability));

                yield getEnumeratedDistribution(probabilities).sample();
            }
        };
    }

    private static double getPeriodicBinaryDistributionStartingFromZero(Integer currentStep) {
        return (1 - cos(PI * currentStep / 2)) / 2;
    }

    private static double getPeriodicBinaryDistributionStartingFromOne(Integer currentStep) {
        return (cos(PI * currentStep / 2) + 1) / 2;
    }

    public static RoomType getRandomClusterConnectionRoomType(Weight expectedWeight,
                                                              Integer currentStep,
                                                              Integer totalRooms) {
        log.debug("Current step: {} / {}", currentStep, totalRooms);
        Double positiveRoomDistribution = getPeriodicBinaryDistributionStartingFromZero(currentStep);
        log.debug("Positive weight treasure room distribution: {}", positiveRoomDistribution);
        Double negativeRoomDistribution = getPeriodicBinaryDistributionStartingFromOne(currentStep);
        log.debug("Negative weight room distribution: {}", negativeRoomDistribution);
        Double normalRoomDistribution = (double) (currentStep / totalRooms);
        log.debug("Current normal room distribution: {}", normalRoomDistribution);

        return switch (getEnumeratedDistribution(List.of(Pair.create(NEGATIVE, negativeRoomDistribution),
                Pair.create(ZERO, normalRoomDistribution), Pair.create(POSITIVE, positiveRoomDistribution))).sample()) {
            case ZERO -> NORMAL;
            case POSITIVE -> {
                List<Pair<RoomType, Double>> probabilities = new ArrayList<>();
                val armorToMaxArmor = expectedWeight.getMaxArmor() == 0 ? expectedWeight.getArmor() :
                        expectedWeight.getArmor() / expectedWeight.getMaxArmor();
                val goldBonus = expectedWeight.getGoldBonus();
                val specialRoomDistribution = getExponentialDistribution(currentStep, 0.0);
                log.debug("Special room distribution: {}", specialRoomDistribution);
                val treasureRoomDistribution =  sqrt(1.0 - (double) (currentStep / totalRooms));
                log.debug("Treasure room distribution: {}", treasureRoomDistribution);


                val merchantProbability = specialRoomDistribution * goldBonus; //TODO: add potential (unequipped items) weight
                log.debug("Merchant probability: {}", merchantProbability);
                val anvilProbability = specialRoomDistribution * armorToMaxArmor;
                val treasureProbability = treasureRoomDistribution * goldBonus; //TODO: add potential (unequipped items) weight
                log.debug("Treasure probability: {}", treasureProbability);

                val sumProb =  merchantProbability + treasureProbability + anvilProbability;

                if (sumProb == 0.0) {
                    yield  NORMAL;
                }

                val positiveProbabilitiesNormalizingFactor =
                        1 / sumProb;

                probabilities.add(Pair.create(RoomType.TREASURE, max(0.0, min(1.0, treasureProbability * positiveProbabilitiesNormalizingFactor))));
                probabilities.add(Pair.create(RoomType.MERCHANT, max(0.0, min(1.0, merchantProbability * positiveProbabilitiesNormalizingFactor))));
                probabilities.add(Pair.create(ANVIL, max(0.0, min(1.0, anvilProbability * positiveProbabilitiesNormalizingFactor))));

                yield getEnumeratedDistribution(probabilities).sample();
            }
            case NEGATIVE -> {
                List<Pair<RoomType, Double>> probabilities = new ArrayList<>();

                val arcaneMagic = expectedWeight.getArcaneMagic();
                val divineMagic = expectedWeight.getDivineMagic();

                if (arcaneMagic == 0 && divineMagic == 0) {
                    yield getEnumeratedDistribution(List.of(
                            Pair.create(WEREWOLF, 0.2),
                            Pair.create(DRAGON, 0.2),
                            Pair.create(VAMPIRE, 0.2),
                            Pair.create(ZOMBIE, 0.2),
                            Pair.create(SWAMP_BEAST, 0.2))).sample();
                }

                val expectedMagicWeightVector = MagicType.of(divineMagic, arcaneMagic);

                val werewolfMagicDiffAbs = expectedMagicWeightVector.toVector()
                        .subtract(getMagicByMonsterType(WEREWOLF).toVector()).getNorm();
                val swampBeastMagicDiffAbs = expectedMagicWeightVector.toVector()
                        .subtract(getMagicByMonsterType(SWAMP_BEAST).toVector()).getNorm();
                val vampireMagicDiffAbs = expectedMagicWeightVector.toVector()
                        .subtract(getMagicByMonsterType(VAMPIRE).toVector()).getNorm();
                val dragonMagicDiffAbs = expectedMagicWeightVector.toVector()
                        .subtract(getMagicByMonsterType(DRAGON).toVector()).getNorm();
                val zombieMagicDiffAbs = expectedMagicWeightVector.toVector()
                        .subtract(getMagicByMonsterType(ZOMBIE).toVector()).getNorm();

                val werewolfRawProbability = werewolfMagicDiffAbs == 0.0 ? 1.0 :  1 / werewolfMagicDiffAbs;
                val swampBeastRawProbability = swampBeastMagicDiffAbs == 0.0 ? 1.0 : 1 / swampBeastMagicDiffAbs;
                val vampireRawProbability = vampireMagicDiffAbs == 0.0 ? 1.0 : 1 / vampireMagicDiffAbs;
                val dragonRawProbability = dragonMagicDiffAbs == 0.0 ? 1.0 : 1/ dragonMagicDiffAbs;
                val zombieRawProbability = zombieMagicDiffAbs == 0.0 ? 1.0 : 1 / zombieMagicDiffAbs;

                val sumProb = werewolfRawProbability + swampBeastRawProbability +
                        vampireRawProbability + dragonRawProbability + zombieRawProbability;

                if (sumProb == 0.0) {
                    yield getEnumeratedDistribution(List.of(
                            Pair.create(WEREWOLF, 0.2),
                            Pair.create(DRAGON, 0.2),
                            Pair.create(VAMPIRE, 0.2),
                            Pair.create(ZOMBIE, 0.2),
                            Pair.create(SWAMP_BEAST, 0.2))).sample();
                }

                val monsterNormalizingFactor = 1 / (sumProb);

                val werewolfProbability = werewolfRawProbability * monsterNormalizingFactor;
                log.debug("Werewolf probability: {}", werewolfProbability);
                val swampBeastProbability =  swampBeastRawProbability * monsterNormalizingFactor;
                log.debug("Swamp beast probability: {}", swampBeastProbability);
                val vampireProbability =  vampireRawProbability * monsterNormalizingFactor;
                log.debug("Vampire probability: {}", vampireProbability);
                val dragonProbability =  dragonRawProbability * monsterNormalizingFactor;
                log.debug("Dragon probability: {}", dragonProbability);
                val zombieProbability =  zombieRawProbability * monsterNormalizingFactor;
                log.debug("Zombie probability: {}", zombieProbability);

                probabilities.add(Pair.create(RoomType.WEREWOLF, werewolfProbability));
                probabilities.add(Pair.create(RoomType.SWAMP_BEAST, swampBeastProbability));
                probabilities.add(Pair.create(RoomType.VAMPIRE, vampireProbability));
                probabilities.add(Pair.create(RoomType.DRAGON, dragonProbability));
                probabilities.add(Pair.create(RoomType.ZOMBIE, zombieProbability));

                yield getEnumeratedDistribution(probabilities).sample();
            }
        };
    }

    public static RoomType getDeadEndRouteRoomType(Weight expectedWeight, int currentStep, double clusterDensity) {
        if (currentStep == 0) {
            return TREASURE;
        } else {
            val negativeRoomDistribution = getPeriodicBinaryDistributionStartingFromOne(currentStep - 1);
            val positiveRoomDistribution = getPeriodicBinaryDistributionStartingFromZero(currentStep - 1);
            val normalRoomDistribution = currentStep / clusterDensity;

            return switch (getEnumeratedDistribution(List.of(Pair.create(NEGATIVE, negativeRoomDistribution),
                    Pair.create(ZERO, normalRoomDistribution), Pair.create(POSITIVE, positiveRoomDistribution))).sample()) {
                case ZERO -> NORMAL;
                case POSITIVE -> {
                    List<Pair<RoomType, Double>> probabilities = new ArrayList<>();
                    val hpToMaxHp =  expectedWeight.getHp() / expectedWeight.getMaxHp();
                    val hpDeficiencyToMaxHp = (expectedWeight.getMaxHp() - expectedWeight.getHp()) / expectedWeight.getMaxHp();
                    val manaToMaxMana = expectedWeight.getMana() / expectedWeight.getMaxMana();
                    val manaDeficiencyToMaxMana = (expectedWeight.getMaxMana() - expectedWeight.getMana()) / expectedWeight.getMaxMana();
                    val armorToMaxArmor = expectedWeight.getArmor() / expectedWeight.getMaxArmor();
                    val goldBonus = expectedWeight.getGoldBonus();
                    val specialRoomDistribution = getExponentialDistribution(currentStep, 0.0);
                    log.debug("Special room distribution: {}", specialRoomDistribution);
                    val treasureRoomDistribution =  sqrt(1.0 - (currentStep / clusterDensity));
                    log.debug("Treasure room distribution: {}", treasureRoomDistribution);
                    val healthShrineDistribution =
                            getExponentialDistribution(currentStep, clusterDensity * hpDeficiencyToMaxHp);
                    log.debug("Health shrine distribution: {}", healthShrineDistribution);
                    val manaShrineDistribution =
                            getExponentialDistribution(currentStep, clusterDensity * manaDeficiencyToMaxMana);
                    log.debug("Mana shrine distribution: {}", manaShrineDistribution);

                    val healthShrineProbability = healthShrineDistribution * hpToMaxHp;
                    log.debug("Health shrine probability: {}", healthShrineProbability);
                    val manaShrineProbability = manaShrineDistribution * manaToMaxMana;
                    log.debug("Mana shrine probability: {}", manaShrineProbability);
                    val merchantProbability = specialRoomDistribution * goldBonus; //TODO: add potential (unequipped items) weight
                    log.debug("Merchant probability: {}", merchantProbability);
                    val anvilProbability = specialRoomDistribution * armorToMaxArmor;
                    val treasureProbability = treasureRoomDistribution * goldBonus; //TODO: add potential (unequipped items) weight
                    log.debug("Treasure probability: {}", treasureProbability);

                    val sumProb = healthShrineProbability + manaShrineProbability + merchantProbability + treasureProbability + anvilProbability;

                    if (sumProb == 0.0) {
                        yield  NORMAL;
                    }

                    val positiveProbabilitiesNormalizingFactor =
                            1 / sumProb;

                    probabilities.add(Pair.create(RoomType.TREASURE, max(0.0, min(1.0, treasureProbability * positiveProbabilitiesNormalizingFactor))));
                    probabilities.add(Pair.create(RoomType.HEALTH_SHRINE, max(0.0, min(1.0, healthShrineProbability * positiveProbabilitiesNormalizingFactor))));
                    probabilities.add(Pair.create(RoomType.MANA_SHRINE, max(0.0, min(1.0, manaShrineProbability * positiveProbabilitiesNormalizingFactor))));
                    probabilities.add(Pair.create(RoomType.MERCHANT, max(0.0, min(1.0, merchantProbability * positiveProbabilitiesNormalizingFactor))));
                    probabilities.add(Pair.create(ANVIL, max(0.0, min(1.0, anvilProbability * positiveProbabilitiesNormalizingFactor))));

                    yield getEnumeratedDistribution(probabilities).sample();
                }
                case NEGATIVE -> {
                    List<Pair<RoomType, Double>> probabilities = new ArrayList<>();

                    val arcaneMagic = expectedWeight.getArcaneMagic();
                    val divineMagic = expectedWeight.getDivineMagic();

                    if (arcaneMagic == 0 && divineMagic == 0) {
                        yield getEnumeratedDistribution(List.of(
                                Pair.create(WEREWOLF, 0.2),
                                Pair.create(DRAGON, 0.2),
                                Pair.create(VAMPIRE, 0.2),
                                Pair.create(ZOMBIE, 0.2),
                                Pair.create(SWAMP_BEAST, 0.2))).sample();
                    }

                    val expectedMagicWeightVector = MagicType.of(divineMagic, arcaneMagic);

                    val werewolfMagicDiffAbs = expectedMagicWeightVector.toVector()
                            .subtract(getMagicByMonsterType(WEREWOLF).toVector()).getNorm();
                    val swampBeastMagicDiffAbs = expectedMagicWeightVector.toVector()
                            .subtract(getMagicByMonsterType(SWAMP_BEAST).toVector()).getNorm();
                    val vampireMagicDiffAbs = expectedMagicWeightVector.toVector()
                            .subtract(getMagicByMonsterType(VAMPIRE).toVector()).getNorm();
                    val dragonMagicDiffAbs = expectedMagicWeightVector.toVector()
                            .subtract(getMagicByMonsterType(DRAGON).toVector()).getNorm();
                    val zombieMagicDiffAbs = expectedMagicWeightVector.toVector()
                            .subtract(getMagicByMonsterType(ZOMBIE).toVector()).getNorm();

                    val werewolfRawProbability = werewolfMagicDiffAbs == 0.0 ? 1.0 :  1 / werewolfMagicDiffAbs;
                    val swampBeastRawProbability = swampBeastMagicDiffAbs == 0.0 ? 1.0 : 1 / swampBeastMagicDiffAbs;
                    val vampireRawProbability = vampireMagicDiffAbs == 0.0 ? 1.0 : 1 / vampireMagicDiffAbs;
                    val dragonRawProbability = dragonMagicDiffAbs == 0.0 ? 1.0 : 1/ dragonMagicDiffAbs;
                    val zombieRawProbability = zombieMagicDiffAbs == 0.0 ? 1.0 : 1 / zombieMagicDiffAbs;

                    val sumProb = werewolfRawProbability + swampBeastRawProbability +
                            vampireRawProbability + dragonRawProbability + zombieRawProbability;

                    if (sumProb == 0.0) {
                        yield getEnumeratedDistribution(List.of(
                                Pair.create(WEREWOLF, 0.2),
                                Pair.create(DRAGON, 0.2),
                                Pair.create(VAMPIRE, 0.2),
                                Pair.create(ZOMBIE, 0.2),
                                Pair.create(SWAMP_BEAST, 0.2))).sample();
                    }

                    val monsterNormalizingFactor = 1 / (sumProb);

                    val werewolfProbability = werewolfRawProbability * monsterNormalizingFactor;
                    log.debug("Werewolf probability: {}", werewolfProbability);
                    val swampBeastProbability =  swampBeastRawProbability * monsterNormalizingFactor;
                    log.debug("Swamp beast probability: {}", swampBeastProbability);
                    val vampireProbability =  vampireRawProbability * monsterNormalizingFactor;
                    log.debug("Vampire probability: {}", vampireProbability);
                    val dragonProbability =  dragonRawProbability * monsterNormalizingFactor;
                    log.debug("Dragon probability: {}", dragonProbability);
                    val zombieProbability =  zombieRawProbability * monsterNormalizingFactor;
                    log.debug("Zombie probability: {}", zombieProbability);

                    probabilities.add(Pair.create(RoomType.WEREWOLF, werewolfProbability));
                    probabilities.add(Pair.create(RoomType.SWAMP_BEAST, swampBeastProbability));
                    probabilities.add(Pair.create(RoomType.VAMPIRE, vampireProbability));
                    probabilities.add(Pair.create(RoomType.DRAGON, dragonProbability));
                    probabilities.add(Pair.create(RoomType.ZOMBIE, zombieProbability));

                    yield getEnumeratedDistribution(probabilities).sample();
                }
            };
        }
    }

    private static Double getExponentialDistribution(Integer a, Double infPoint) {
        return switch (a.compareTo(infPoint.intValue())) {
                case 0 -> 1.0 ;
                case 1 -> exp(a - infPoint);
                case -1 -> exp(infPoint - a);
            default -> 0.0;
        };
    }

    public static MagicType getMagicTypeByMonsterClass(MonsterClass monsterClass) {
        return switch (monsterClass) {
            case WEREWOLF -> MagicType.of(1/sqrt(2), -1/sqrt(2));
            case DRAGON -> MagicType.of(1/sqrt(2), 1/sqrt(2));
            case VAMPIRE -> MagicType.of(-1/sqrt(2), 1/sqrt(2));
            case ZOMBIE -> MagicType.of(-1.0, 0.0);
            case SWAMP_BEAST -> MagicType.of(-1/sqrt(2), -1/sqrt(2));
        };
    }

    public static MagicType getMagicByMonsterType(RoomType monsterType) {
        return getMagicTypeByMonsterClass(convertToMonsterClass(monsterType));
    }

    private static <T>EnumeratedDistribution<T> getEnumeratedDistribution(List<Pair<T, Double>> probabilities) {
        return new EnumeratedDistribution<>(probabilities);
    }
}
