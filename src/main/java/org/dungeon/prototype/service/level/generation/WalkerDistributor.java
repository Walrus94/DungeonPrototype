package org.dungeon.prototype.service.level.generation;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.level.generation.LevelGridCluster;
import org.dungeon.prototype.model.level.generation.NextRoomDto;
import org.dungeon.prototype.model.level.ui.GridSection;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.service.UniqueIdFactory;

import java.util.*;

import static java.util.Objects.nonNull;
import static org.dungeon.prototype.service.level.generation.WalkerDistributor.Status.FINISHED;
import static org.dungeon.prototype.service.level.generation.WalkerDistributor.Status.RUNNING;
import static org.dungeon.prototype.service.level.generation.WalkerDistributor.Status.WAITING;
import static org.dungeon.prototype.util.LevelUtil.getAdjacentSectionsInCluster;
import static org.dungeon.prototype.util.LevelUtil.setMutualAdjacency;

@Data
@Slf4j
@Builder
public class WalkerDistributor {
    @Builder.Default
    private long id = UniqueIdFactory.getInstance().getNextId();
    private long chatId;
    private LevelGridCluster cluster;
    private boolean runSubWalkerOnRouteFork;
    @Builder.Default
    private List<WalkerDistributor> subWalkers = new ArrayList<>();
    private Room previousRoom;
    private GridSection currentSection;
    private int currentStep;
    @Builder.Default
    private int totalSteps = 0;
    private int mainPathLength;
    private Status status;

    public NextRoomDto nextStep(GridSection[][] grid, Map<Point, Room> roomsMap) {
        log.info("Distribute walker id{}, status:{} next step...", id, status);
        log.debug("Current step: {}, total steps: {}, main path length:{}", currentStep, totalSteps, mainPathLength);
        log.debug("Current section: {}, cluster: {}", currentSection, cluster);
        val adjacentSections = getAdjacentSectionsInCluster(currentSection.getPoint(), grid, cluster);
        if (nonNull(previousRoom)) {
            adjacentSections.removeIf(section -> section.getPoint().equals(previousRoom.getPoint()));
        }
        adjacentSections.removeIf(section -> roomsMap.containsKey(section.getPoint()) || section.getStepsFromStart() == 0);
        GridSection nextSection = null;
        if (adjacentSections.size() > 1 && !runSubWalkerOnRouteFork && !currentSection.isConnectionPoint()) {
            log.info("Running sub-walker on route fork disabled...");
            log.info("Finishing walker. Running sub-walkers remaining: {}", subWalkers.size());
            finishWalker(grid, roomsMap);
        } else {
            log.info("Assigning next section...");
            Optional<GridSection> nextSectionOptional = adjacentSections.stream()
                    .filter(section -> section.getStepsFromStart() == currentStep)
                    .min(Comparator.comparing(section -> getAdjacentSectionsInCluster(section.getPoint(), grid, cluster).size()));
            if (nextSectionOptional.isPresent()) {
                nextSection = nextSectionOptional.get();
                log.info("Next section: {}", nextSection);
            } else {
                log.info("Finishing walker...");
                finishWalker(grid, roomsMap);
                return null;
            }
        }

        if (nonNull(nextSection)) {
            var room = buildRoom(nextSection, chatId);
            setMutualAdjacency(room, previousRoom);
            if (runSubWalkerOnRouteFork && adjacentSections.size() > 1) {
                log.info("Running sub-walker on route fork...");
                for (GridSection section : adjacentSections) {
                    if (!section.getPoint().equals(nextSection.getPoint()) && !roomsMap.containsKey(section.getPoint())) {
                        if (currentSection.isConnectionPoint() || section.getStepsFromStart() == currentStep) {
                            subWalkers.add(WalkerDistributor.builder()
                                    .chatId(chatId)
                                    .cluster(cluster)
                                    .runSubWalkerOnRouteFork(true)
                                    .status(RUNNING)
                                    .currentSection(currentSection)
                                    .previousRoom(roomsMap.get(currentSection.getPoint()))
                                    .currentStep(section.getStepsFromStart())
                                    .totalSteps(section.getStepsFromStart())
                                    .build());
                        }
                    }
                }
                log.debug("Sub-walkers: {}", subWalkers);
            }
            currentSection = nextSection;
            previousRoom = room;
            currentStep--;
            if (currentStep == 0) {
                log.info("Current step equals 0, finishing walker");
                if (subWalkers.isEmpty()) {
                    log.info("No sub-walkers present, finishing walker...");
                    status = Status.FINISHED;
                } else {
                    status = Status.WAITING;
                    log.info("Sub-walkers left: {}", subWalkers.size());
                }
            }
            return NextRoomDto.builder()
                    .room(room)
                    .section(nextSection)
                    .currentStep(currentStep)
                    .totalSteps(totalSteps)
                    .build();
        } else {
            return null;
        }
    }

    private void finishWalker(GridSection[][] grid, Map<Point, Room> roomsMap) {
        getAdjacentSectionsInCluster(currentSection.getPoint(), grid, cluster).stream()
                .filter(section -> roomsMap.containsKey(section.getPoint()) && section.getStepsFromStart() == currentStep)
                .findFirst().ifPresent(section ->
                        setMutualAdjacency(roomsMap.get(section.getPoint()), previousRoom));
        if (subWalkers.isEmpty()) {
            log.info("No sub-walkers present, finishing walker...");
            status = Status.FINISHED;
        } else {
            status = Status.WAITING;
            log.info("Sub-walkers left: {}", subWalkers.size());
        }
    }

    private Room buildRoom(GridSection section, Long chatId) {
        log.info("Building room on {}", section.getPoint());
        val room = new Room();
        room.setChatId(chatId);
        room.setPoint(section.getPoint());
        return room;
    }

    public boolean isRunning() {
        return RUNNING.equals(status);
    }

    public boolean isWaiting() {
        return WAITING.equals(status);
    }

    public boolean finished() {
        return FINISHED.equals(status);
    }

    public enum Status {
        RUNNING, WAITING, FINISHED
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WalkerDistributor walkerDistributor)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return this.id == walkerDistributor.getId() &&
                this.chatId == walkerDistributor.getChatId() &&
                this.cluster.equals(walkerDistributor.getCluster()) &&
                this.runSubWalkerOnRouteFork == walkerDistributor.isRunSubWalkerOnRouteFork() &&
                this.subWalkers.equals(walkerDistributor.getSubWalkers()) &&
                this.previousRoom.equals(walkerDistributor.getPreviousRoom()) &&
                this.currentSection == walkerDistributor.getCurrentSection() &&
                this.currentStep == walkerDistributor.getCurrentStep() &&
                this.totalSteps == walkerDistributor.getTotalSteps() &&
                this.mainPathLength == walkerDistributor.getMainPathLength() &&
                this.status.equals(walkerDistributor.getStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, chatId, cluster, runSubWalkerOnRouteFork, subWalkers, previousRoom, currentSection, currentStep,
                totalSteps, mainPathLength, status);
    }

    @Override
    public String toString() {
        return "id=" + id +
                ", chatId=" + chatId +
                ", cluster=" + cluster +
                ", runSubWalkerOnRouteFork=" + runSubWalkerOnRouteFork +
                ", subWalkers=" + subWalkers +
                ", previousRoom=" + previousRoom +
                ", currentSection=" + currentSection +
                ", currentStep" + currentStep +
                ", totalSteps" + totalSteps +
                ", mainPathLength" + mainPathLength +
                ", status" + status;
    }
}
