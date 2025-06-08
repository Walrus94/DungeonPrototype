package org.dungeon.prototype.model.level.generation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.level.ui.GridSection;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.service.UniqueIdFactory;
import org.dungeon.prototype.service.level.generation.WalkerBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.apache.commons.math3.util.FastMath.abs;
import static org.dungeon.prototype.util.RandomUtil.getRandomInt;

@Data
@Slf4j
public class LevelGridCluster {
    private long id;
    private Point startConnectionPoint;
    private Point endConnectionPoint;
    int size = 0;
    int negativeRoomsCount = 0;
    List<GridSection> deadEnds = new ArrayList<>();
    Weight clusterExpectedWeight;
    List<WalkerBuilder> walkers = new ArrayList<>();
    public LevelGridCluster(Point startConnectionPoint, Point endConnectionPoint) {
        this.id = UniqueIdFactory.getInstance().getNextId();
        this.startConnectionPoint = startConnectionPoint;
        this.endConnectionPoint = endConnectionPoint;
    }

    public void incrementSize() {
        size++;
    }
    public double getDensity() {
        return size /
                ((double) abs(endConnectionPoint.getX() - startConnectionPoint.getX()) *
                        abs(endConnectionPoint.getY() - startConnectionPoint.getY()));
    }

    public boolean hasSmallSide() {
        return (abs(endConnectionPoint.getX() - startConnectionPoint.getX()) < 4) ||
                (abs(endConnectionPoint.getY() - startConnectionPoint.getY()) < 4);
    }

    public boolean isSmallCluster() {
        return (abs(endConnectionPoint.getX() - startConnectionPoint.getX()) < 4) &&
                (abs(endConnectionPoint.getY() - startConnectionPoint .getY()) < 4);
    }

    public boolean hasNegativeRooms() {
        return negativeRoomsCount > 0;
    }

    public void incrementNegativeRoomsCount() {
        negativeRoomsCount++;
    }

    public void decrementNegativeRoomsCount() {
        negativeRoomsCount--;
    }

    public boolean hasDeadEnds() {
        return deadEnds.size() > 0;
    }

    public void addDeadEnd(GridSection section) {
        deadEnds.add(section);
    }

    public void removeDeadEnd(GridSection deadEnd) {
        deadEnds.remove(deadEnd);
    }

    public void addDeadEnds(List<GridSection> processedDeadEnds) {
        deadEnds.addAll(processedDeadEnds);
    }

    public void initializeWalkers() {
        log.info("Initializing walkers for cluster {}", id);
        walkers = new ArrayList<>();
        if (isSmallCluster()) {
            log.info("Small cluster, adding two border walkers...");
            walkers = Arrays.asList(WalkerBuilder.builder()
                            .pathFromStart(0)
                            .isReversed(false)
                            .cluster(this)
                            .longestPathDefault(true)
                            .currentPoint(new Point(0, 0))
                            .build(),
                    WalkerBuilder.builder()
                            .pathFromStart(0)
                            .isReversed(false)
                            .longestPathDefault(true)
                            .cluster(this)
                            .currentPoint(new Point(0, 0))
                            .build());
            return;
        }
        if (hasSmallSide()) {
            log.info("Small sided cluster, adding start and end walkers...");
            walkers = Arrays.asList(WalkerBuilder.builder()
                            .pathFromStart(0)
                            .isReversed(false)
                            .cluster(this)
                            .longestPathDefault(false)
                            .currentPoint(new Point(0, 0))
                            .build(),
                    WalkerBuilder.builder()
                            .isReversed(true)
                            .longestPathDefault(true)
                            .pathFromStart(0)
                            .cluster(this)
                            .currentPoint(new Point(endConnectionPoint.getX() - startConnectionPoint.getX(),
                                    endConnectionPoint.getY() - startConnectionPoint.getY()))
                            .build());
            return;
        }
        int fromStartWalkersNumber = getRandomInt(1, 2);
        int fromEndWalkersNumber = getRandomInt(1, 3 - fromStartWalkersNumber);
        log.info("Adding {} walkers to start of cluster, {} walkers to end of cluster", fromStartWalkersNumber, fromEndWalkersNumber);
        IntStream.range(0, fromStartWalkersNumber + fromEndWalkersNumber).forEach(i -> {
            if (i < fromStartWalkersNumber) {
                walkers.add(WalkerBuilder.builder()
                        .pathFromStart(0)
                        .isReversed(false)
                        .longestPathDefault(fromStartWalkersNumber == 2 && i == 0)
                        .cluster(this)
                        .currentPoint(new Point(0, 0))
                        .build());
            } else {
                walkers.add(WalkerBuilder.builder()
                        .isReversed(true)
                        .pathFromStart(0)
                        .longestPathDefault(fromEndWalkersNumber == 1 || i == 2)
                        .cluster(this)
                        .currentPoint(new Point(endConnectionPoint.getX() - startConnectionPoint.getX(),
                                endConnectionPoint.getY() - startConnectionPoint.getY()))
                        .build());
            }
        });
    }

    public void reset() {
        size = 0;
        negativeRoomsCount = 0;
        deadEnds = new ArrayList<>();
        clusterExpectedWeight = null;
        walkers = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "LevelGridCluster{" +
                "id=" + id +
                ", startConnectionPoint=" + startConnectionPoint +
                ", endConnectionPoint=" + endConnectionPoint +
                ", size=" + size +
                ", negativeRoomsCount=" + negativeRoomsCount +
                ", deadEnds=" + deadEnds +
                ", clusterExpectedWeight=" + clusterExpectedWeight +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LevelGridCluster that = (LevelGridCluster) o;
        return id == that.id &&
                size == that.size &&
                negativeRoomsCount == that.negativeRoomsCount &&
                Objects.equals(startConnectionPoint, that.startConnectionPoint) &&
                Objects.equals(endConnectionPoint, that.endConnectionPoint) &&
                Objects.equals(deadEnds, that.deadEnds) &&
                Objects.equals(clusterExpectedWeight, that.clusterExpectedWeight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startConnectionPoint, endConnectionPoint, size, negativeRoomsCount, deadEnds, clusterExpectedWeight);
    }
}
