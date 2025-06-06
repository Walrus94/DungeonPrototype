package org.dungeon.prototype.service.level.generation;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.level.generation.LevelGridCluster;
import org.dungeon.prototype.model.level.ui.GridSection;
import org.dungeon.prototype.service.WalkerUniqueIdFactory;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.dungeon.prototype.util.LevelUtil.getAdjacentSections;
import static org.dungeon.prototype.util.LevelUtil.getAdjacentSectionsInCluster;
import static org.dungeon.prototype.util.LevelUtil.isPointInCluster;


@Data
@Slf4j
@Builder
public class WalkerBuilder {
    @Builder.Default
    private Long id = WalkerUniqueIdFactory.getInstance().getNextId();
    private Point previousPoint;
    private Point currentPoint;
    private boolean isReversed;
    @Builder.Default
    private boolean overridingReversedPath = false;
    private int pathToFinish;
    @Builder.Default
    private boolean longestPathDefault = false;
    @Builder.Default
    private boolean stopped = false;
    @Builder.Default
    private int pathFromStart = 0;
    private LevelGridCluster cluster;

    public void nextStep(GridSection[][] grid) {
        log.info("Walker id:{} (reversed:{}, overriding:{}, border path:{}) next step...",
                id, isReversed, overridingReversedPath, longestPathDefault);
        GridSection currentSection = grid[currentPoint.getX()][currentPoint.getY()];
        log.debug("Current section: {}", currentSection);

        val nextGridSectionOptional = selectNextStep(grid);
        if (nextGridSectionOptional.isPresent()) {
            GridSection nextGridSection = nextGridSectionOptional.get();
            log.debug("Next grid section: {}", nextGridSection);
            previousPoint = currentPoint;
            currentPoint = nextGridSection.getPoint();
            log.info("Adding grid section to point {}", currentPoint);
            if (!isReversed && !overridingReversedPath) {
                if (nextGridSection.getStepsFromStart() < 0) {
                    log.info("Reversed walker trace met, switching to overriding reversed path mode...");
                    overridingReversedPath = true;
                    pathToFinish = nextGridSection.getStepsFromStart();
                }
                pathFromStart++;
                cluster.incrementSize();
                nextGridSection.setStepsFromStart(pathFromStart);
            } else {
                if (isReversed) {
                    pathFromStart--;
                    nextGridSection.setStepsFromStart(pathFromStart);
                    cluster.incrementNegativeRoomsCount();
                    log.info("Setting next point in reversed mode...");
                } else {
                    cluster.decrementNegativeRoomsCount();
                    pathToFinish++;
                    pathFromStart++;
                    nextGridSection.setStepsFromStart(pathFromStart);
                    cluster.incrementSize();
                    log.info("Setting next point, path from start: {}", pathFromStart);
                }
            }
            grid[currentPoint.getX()][currentPoint.getY()] = nextGridSection;
        } else {
            stopped = true;
            log.info("Stopped walker id:{}", this.id);
            if (!isReversed && !overridingReversedPath) {
                if (getAdjacentSectionsInCluster(currentPoint, grid, cluster).stream()
                        .noneMatch(section -> cluster.getEndConnectionPoint().equals(section.getPoint()))) {
                    currentSection.setDeadEnd(true);
                    cluster.addDeadEnd(currentSection);
                }
                log.info("Stopped walker, setting dead end to {}", currentSection);
            }
        }
    }

    private Optional<GridSection> selectNextStep(GridSection[][] grid) {
        log.info("Choosing next step...");
        Set<GridSection> adjacentSections = getAdjacentSectionsInCluster(currentPoint, grid, cluster);
        log.info("{} adjacent sections in cluster {}: {}", currentPoint, cluster, adjacentSections);
        if (isReversed) {
            if (adjacentSections.stream().anyMatch(section -> cluster.getStartConnectionPoint().equals(section.getPoint()))) {
                log.info("Reversed walker reached start of cluster, switching to overriding mode");
                overridingReversedPath = true;
                isReversed = false;
                pathFromStart = 0;
                val nextSection = grid[currentPoint.getX()][currentPoint.getY()];
                pathToFinish = nextSection.getStepsFromStart();
                return Optional.of(nextSection);
            }
            if (adjacentSections.stream().anyMatch(section -> section.getStepsFromStart() > 0 &&
                    !currentPoint.equals(section.getPoint()) &&
                    !cluster.getEndConnectionPoint().equals(section.getPoint()))) {
                log.info("Reversed walker have adjacent section visited, switching to override mode");
                overridingReversedPath = true;
                isReversed = false;
                val foundSection = adjacentSections.stream().filter(section -> section.getStepsFromStart() > 0 &&
                        !currentPoint.equals(section.getPoint()) &&
                        !cluster.getEndConnectionPoint().equals(section.getPoint())).findFirst().get();
                pathFromStart = foundSection.getStepsFromStart();
                foundSection.setDeadEnd(false);
                cluster.removeDeadEnd(foundSection);
                val nextSection = grid[currentPoint.getX()][currentPoint.getY()];
                pathToFinish = nextSection.getStepsFromStart();
                return Optional.of(nextSection);
            }
            if (longestPathDefault) {
                return adjacentSections.stream()
                        .filter(section -> !currentPoint.equals(section.getPoint()))
                        .filter(section -> section.getStepsFromStart() == 0)
                        .filter(section -> !cluster.getEndConnectionPoint().equals(section.getPoint()))
                        .max(Comparator.comparing(section -> getAdjacentSections(section.getPoint(), grid)
                                .stream()
                                .filter(adjacentSection -> !isPointInCluster(adjacentSection.getPoint(), cluster))
                                .count()));
            }
            return adjacentSections.stream()
                    .filter(section -> !currentPoint.equals(section.getPoint()))
                    .filter(section -> section.getStepsFromStart() == 0)
                    .filter(section -> !cluster.getEndConnectionPoint().equals(section.getPoint()))
                    .findAny();
        } else {
            if (overridingReversedPath) {
                if (pathToFinish == 0 && adjacentSections.stream()
                        .anyMatch(section -> cluster.getEndConnectionPoint().equals(section.getPoint()))) {
                    log.info("Cluster end reached");
                    return Optional.empty();
                }
                log.info("Overriding reversed path, current step:{}, path to finish:{}...", pathFromStart, pathToFinish);
                return adjacentSections.stream()
                        .filter(gridSection -> gridSection.getStepsFromStart() == pathToFinish &&
                                !currentPoint.equals(gridSection.getPoint()))
                        .findFirst();
            } else {
                if (adjacentSections.stream().anyMatch(section -> cluster.getEndConnectionPoint().equals(section.getPoint()))) {
                    log.info("Cluster end reached");
                    return Optional.empty();
                }
                if (longestPathDefault) {
                    return adjacentSections.stream()
                            .filter(section -> section.getStepsFromStart() == 0)
                            .filter(section -> !cluster.getStartConnectionPoint().equals(section.getPoint()))
                            .max(Comparator.comparing(section -> getAdjacentSections(section.getPoint(), grid)
                                    .stream()
                                    .filter(adjacentSection -> !isPointInCluster(adjacentSection.getPoint(), cluster))
                                    .count()));
                }
                return adjacentSections.stream()
                        .filter(section -> section.getStepsFromStart() == 0)
                        .filter(section -> !cluster.getStartConnectionPoint().equals(section.getPoint()))
                        .min(Comparator.comparing(gridSection ->
                                getAdjacentSectionsInCluster(gridSection.getPoint(), grid, cluster)
                                        .stream()
                                        .filter(section -> section.getStepsFromStart() > 0)
                                        .count()));
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WalkerBuilder walkerBuilder)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return this.id.equals(walkerBuilder.getId()) &&
                this.currentPoint.equals(walkerBuilder.getCurrentPoint()) &&
                this.isReversed == walkerBuilder.isReversed() &&
                this.overridingReversedPath == walkerBuilder.isOverridingReversedPath() &&
                this.longestPathDefault == walkerBuilder.isLongestPathDefault() &&
                this.stopped == walkerBuilder.isStopped() &&
                this.pathFromStart == walkerBuilder.getPathFromStart() &&
                this.cluster.equals(walkerBuilder.cluster);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, currentPoint, isReversed, overridingReversedPath, longestPathDefault, stopped, pathFromStart, cluster);
    }

    @Override
    public String toString() {
        return "id=" + id +
                ", currentPoint=" + currentPoint +
                ", isReversed=" + isReversed +
                ", overridingReversedPath=" + overridingReversedPath +
                ", longestPathDefault= " + longestPathDefault +
                ", stopped=" + stopped +
                ", pathFromStart=" + pathFromStart +
                ", cluster" + cluster;
    }
}
