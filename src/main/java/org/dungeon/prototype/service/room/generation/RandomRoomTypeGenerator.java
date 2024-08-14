package org.dungeon.prototype.service.room.generation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.RoomsSegment;
import org.dungeon.prototype.model.room.content.*;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.util.LevelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static org.apache.commons.math3.util.FastMath.abs;
import static org.dungeon.prototype.model.room.RoomType.HEALTH_SHRINE;
import static org.dungeon.prototype.model.room.RoomType.MANA_SHRINE;
import static org.dungeon.prototype.model.room.RoomType.MERCHANT;
import static org.dungeon.prototype.model.room.RoomType.NORMAL;
import static org.dungeon.prototype.model.room.RoomType.TREASURE;
import static org.dungeon.prototype.util.RandomUtil.getNextRoomType;
import static org.dungeon.prototype.util.RoomGenerationUtils.calculateExpectedRange;
import static org.dungeon.prototype.util.RoomGenerationUtils.calculateExpectedWeightAbs;
import static org.dungeon.prototype.util.RoomGenerationUtils.getMonsterRoomTypes;

@Slf4j
@Component
public class RandomRoomTypeGenerator {
    @Autowired
    private RoomContentRandomFactory roomContentRandomFactory;
    @Autowired
    private MonsterRoomGenerationService monsterRoomGenerationService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private EffectService effectService;
    public LevelRoomTypeClusters generateClusters(Level level, Player player) {
        log.debug("Initializing room type generator parameters...");
        val levelRoomTypeClusters = new LevelRoomTypeClusters();
        levelRoomTypeClusters.setRoomsLeft(level.getRoomsMap().size() - 2);
        levelRoomTypeClusters.setChatId(level.getChatId());
        levelRoomTypeClusters.setTotalRooms(levelRoomTypeClusters.getRoomsLeft());
        levelRoomTypeClusters.setMonsterClassesCounters(Arrays.stream(MonsterClass.values()).collect(Collectors.toMap(Function.identity(), attackType -> 0)));
        levelRoomTypeClusters.setRoomTreasures(0);
        levelRoomTypeClusters.setHealthShrineInitialProbability((double) (player.getMaxHp() - player.getHp()));
        levelRoomTypeClusters.setManaShrineInitialProbability((double) player.getMaxMana() - player.getMana());
        levelRoomTypeClusters.setMerchantInitialProbability((double) player.getInventory().getMaxItems() - player.getInventory().getItems().size());
        levelRoomTypeClusters.setHasHealthShrineRoom(false);
        levelRoomTypeClusters.setHasManaShrineRoom(false);
        levelRoomTypeClusters.setHasMerchantRoom(false);
        levelRoomTypeClusters.setUsedItemIds(new HashSet<>());
        levelRoomTypeClusters.setDeadEnds(new PriorityQueue<>(Comparator.comparing(GridSection::getStepsFromStart)));
        level.getDeadEnds().forEach(levelRoomTypeClusters.getDeadEnds()::offer);
        levelRoomTypeClusters.setDeadEndToSegmentMap(level.getDeadEndToSegmentMap());
        levelRoomTypeClusters.setMainSegment(LevelUtil.getMainSegment(level));
        log.debug("Parameters - levelNumber:{}, deadEnds:{}, roomMonsters: {}, roomTreasures: {},  total: {}",
                level.getNumber(), levelRoomTypeClusters.getDeadEnds().size(),
                levelRoomTypeClusters.getMonsterClassesCounters(), levelRoomTypeClusters.getRoomTreasures(),
                levelRoomTypeClusters.getRoomsLeft());
        log.debug("Start generating clusters...");
        Map<RoomsSegment, RoomTypesCluster> clusters = new HashMap<>();
        var expectedRange = calculateExpectedRange(player);
        var expectedWeightAbs = calculateExpectedWeightAbs(player);
        while (levelRoomTypeClusters.getRoomsLeft() > 0) {
            val deadEnds = levelRoomTypeClusters.getDeadEnds();
            log.debug("deadEnds: {}", deadEnds);
            log.debug("deadEndToSegmentMap: {}", levelRoomTypeClusters.getDeadEndToSegmentMap());
            val noDeadEndLeft = deadEnds.isEmpty();
            if (noDeadEndLeft) {
                log.debug("Out of dead ends! Proceeding to end room...");
            }
            GridSection deadEnd;
            RoomsSegment segment;
            int clusterSize;
            if (noDeadEndLeft) {
                deadEnd = levelRoomTypeClusters.getMainSegment().getEnd();
                segment = levelRoomTypeClusters.getMainSegment();
                clusterSize = deadEnd.getStepsFromStart() - 1;
            } else {
                deadEnd = deadEnds.poll();
                segment = levelRoomTypeClusters.getDeadEndToSegmentMap().get(deadEnd.getPoint());
                clusterSize = segment.getEnd().getStepsFromStart() - segment.getStart().getStepsFromStart();
            }
            log.debug("Current room: {}, current segment: {}", deadEnd.getPoint(), segment);
            val nextCluster = generateCluster(clusterSize, expectedRange, expectedWeightAbs, levelRoomTypeClusters);
            expectedRange = nextCluster.getMiddleAbsWeight();
            log.debug("Current expected weight range: {} , expected weight (abs): {}", expectedRange, expectedWeightAbs);
            clusters.put(segment, nextCluster);
            levelRoomTypeClusters.setRoomsLeft(levelRoomTypeClusters.getRoomsLeft() - clusterSize);
            if (deadEnds.isEmpty() && levelRoomTypeClusters.getRoomsLeft() > 0) {
                log.warn("WARNING! Undistributed rooms left: {}!", levelRoomTypeClusters.getRoomsLeft());
            }
        }
        log.debug("Generated clusters: {}", clusters);
        levelRoomTypeClusters.setClusters(clusters);
        return levelRoomTypeClusters;
    }

    public RoomTypesCluster generateCluster(int totalRooms, int expectedRange, Integer expectedWeightAbs, LevelRoomTypeClusters levelRoomTypeClusters) {
        log.debug("Generating cluster size {}...", totalRooms);
        log.debug("Current expected weight range: {}", expectedRange);
        var cluster = new RoomTypesCluster(totalRooms);
        var lastAddedWeight = 0;
        var middleWeight = 0;
        RoomContent roomContent;
        while (cluster.hasRoomLeft()) {
            log.debug("Next expected weight absolute value: {}", expectedWeightAbs);
            var exclude = getExcludedSpecialRoomTypes(levelRoomTypeClusters);
            log.debug("Room types to exclude: {}", exclude);
            val currentStep = totalRooms - cluster.getTotalRooms() + 1;
            if (abs(lastAddedWeight) < expectedRange) {
                log.debug("Generating random room...");
                roomContent = nextRoomContent(exclude, currentStep, expectedWeightAbs, levelRoomTypeClusters);
            } else {
                if (middleWeight <= 0) {
                    log.debug("Positive weight room generation...");
                    var excludeNegativeAndZero = new HashSet<>(exclude);
                    excludeNegativeAndZero.addAll(getMonsterRoomTypes());
                    excludeNegativeAndZero.add(NORMAL);
                    roomContent = nextRoomContent(excludeNegativeAndZero, currentStep, expectedWeightAbs, levelRoomTypeClusters);
                } else {
                    log.debug("Negative weight room generation...");
                    var excludePositiveAndZero = new HashSet<>(exclude);
                    excludePositiveAndZero.addAll(Set.of(HEALTH_SHRINE, MANA_SHRINE, MERCHANT, TREASURE, NORMAL));
                    roomContent = nextRoomContent(excludePositiveAndZero, currentStep, expectedWeightAbs, levelRoomTypeClusters);
                    if (getMonsterRoomTypes().contains(roomContent.getRoomType())) {
                        val monster = ((MonsterRoom) roomContent).getMonster();
                        val monsterClass = monster.getMonsterClass();
                        val value = levelRoomTypeClusters.getMonsterClassesCounters().get(monsterClass) + 1;
                        levelRoomTypeClusters.getMonsterClassesCounters().put(monsterClass, value);
                    }
                }
            }
            lastAddedWeight = cluster.addRoom(roomContent);
            middleWeight = cluster.getMiddleWeight();
            log.debug("Last added weight: {}", lastAddedWeight);
            log.debug("Added room content, {}", roomContent);
            expectedWeightAbs =  cluster.getMiddleAbsWeight();
        }
        log.debug("Generated cluster: {}", cluster);
        return cluster;
    }


    private Set<RoomType> getExcludedSpecialRoomTypes(LevelRoomTypeClusters levelRoomTypeClusters) {
        Set<RoomType> exclude = new HashSet<>();
        if (levelRoomTypeClusters.isHasHealthShrineRoom()) {
            exclude.add(RoomType.HEALTH_SHRINE);
            log.debug("Health shrine already on the level");
        }
        if (levelRoomTypeClusters.isHasManaShrineRoom()) {
            exclude.add(RoomType.MANA_SHRINE);
            log.debug("Mana shrine already on the level");
        }
        if (levelRoomTypeClusters.isHasMerchantRoom()) {
            exclude.add(RoomType.MERCHANT);
            log.debug("Merchant is already on the level");
        }
        return exclude;
    }

    public RoomContent nextRoomContent(Set<RoomType> exclude, int currentStep, Integer expectedWeightAbs, LevelRoomTypeClusters levelRoomTypeClusters) {
        RoomType roomType;
        if (expectedWeightAbs == 0) {
            return new NormalRoom();
        }
        exclude.addAll(monsterRoomGenerationService.getExcludedMonsters(expectedWeightAbs));
        if (!exclude.isEmpty()) {
            log.debug("Excluded room types: {}", exclude);
            log.debug("Generating next room type...");
            val probabilities = getExcludeTypesProbabilities(exclude);
            roomType = getNextRoomType(levelRoomTypeClusters, currentStep, probabilities);
            log.debug("Random room type: {}", roomType);
        } else {
            log.debug("Generating next room type...");
            roomType = getNextRoomType(levelRoomTypeClusters, currentStep, Collections.emptyMap());
            log.debug("Random room type: {}", roomType);
        }
        log.debug("Selected room type: {}", roomType);
        adjustCountersAndFlags(roomType, expectedWeightAbs, levelRoomTypeClusters);
        val roomContent = roomContentRandomFactory.getNextRoomContent(levelRoomTypeClusters, roomType, expectedWeightAbs);
        if (Set.of(TREASURE, MERCHANT).contains(roomContent.getRoomType())) {
            Set<Item> items = switch (roomContent.getRoomType()) {
                case TREASURE -> ((Treasure) roomContent).getItems();
                case MERCHANT -> ((Merchant) roomContent).getItems();
                default -> emptySet();
            };
            levelRoomTypeClusters.addUsedItemsIds(items.stream().map(Item::getId).collect(Collectors.toSet()));
        }
        return roomService.saveOrUpdateRoomContent(roomContent);
    }

    private Map<RoomType, Double> getExcludeTypesProbabilities(Set<RoomType> exclude) {
        return exclude.stream().collect(Collectors.toMap(Function.identity(), roomType -> 0.0));
    }

    private LevelRoomTypeClusters adjustCountersAndFlags(RoomType roomType, Integer expectedWeightAbs, LevelRoomTypeClusters levelRoomTypeClusters) {
        val monsterClassesCounters = levelRoomTypeClusters.getMonsterClassesCounters();
        switch (roomType) {
            case WEREWOLF -> {
                if (expectedWeightAbs > 0) monsterClassesCounters.put(MonsterClass.WEREWOLF, monsterClassesCounters.get(MonsterClass.WEREWOLF) + 1);
            }
            case VAMPIRE -> {
                if (expectedWeightAbs > 0) monsterClassesCounters.put(MonsterClass.VAMPIRE, monsterClassesCounters.get(MonsterClass.VAMPIRE) + 1);
            }
            case SWAMP_BEAST -> {
                if (expectedWeightAbs > 0) monsterClassesCounters.put(MonsterClass.SWAMP_BEAST, monsterClassesCounters.get(MonsterClass.SWAMP_BEAST) + 1);
            }
            case DRAGON -> {
                if (expectedWeightAbs > 0) monsterClassesCounters.put(MonsterClass.DRAGON, monsterClassesCounters.get(MonsterClass.DRAGON) + 1);
            }
            case ZOMBIE -> {
                if (expectedWeightAbs > 0) monsterClassesCounters.put(MonsterClass.ZOMBIE, monsterClassesCounters.get(MonsterClass.ZOMBIE) + 1);
            }
            case TREASURE -> {
                if (expectedWeightAbs > 0) levelRoomTypeClusters.setRoomTreasures(levelRoomTypeClusters.getRoomTreasures() + 1);
            }
            case MERCHANT -> {
                if (expectedWeightAbs > 0) levelRoomTypeClusters.setHasMerchantRoom(true);
            }
            case HEALTH_SHRINE -> levelRoomTypeClusters.setHasHealthShrineRoom(true);
            case MANA_SHRINE -> levelRoomTypeClusters.setHasManaShrineRoom(true);
        }
        levelRoomTypeClusters.setRoomsLeft(levelRoomTypeClusters.getRoomsLeft() - 1);
        return levelRoomTypeClusters;
    }

    public void updateDeadEndsForDistribution(Level level, LevelRoomTypeClusters clusters) {
        clusters.getDeadEndToSegmentMap().keySet().stream()
                .map(point -> level.getGrid()[point.getX()][point.getY()])
                .forEach(clusters.getDeadEnds()::offer);
    }

}
