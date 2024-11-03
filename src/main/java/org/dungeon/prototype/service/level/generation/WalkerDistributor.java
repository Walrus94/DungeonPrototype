package org.dungeon.prototype.service.level.generation;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.level.generation.LevelGridCluster;
import org.dungeon.prototype.model.level.generation.NextRoomDto;
import org.dungeon.prototype.model.level.ui.GridSection;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.service.WalkerUniqueIdFactory;

import java.util.*;

import static java.util.Objects.nonNull;
import static org.dungeon.prototype.util.LevelUtil.getAdjacentSectionsInCluster;
import static org.dungeon.prototype.util.LevelUtil.getDirection;
import static org.dungeon.prototype.util.LevelUtil.getOppositeDirection;

@Data
@Slf4j
@Builder
public class WalkerDistributor {
    @Builder.Default
    private Long id = WalkerUniqueIdFactory.getInstance().getNextId();
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

    public NextRoomDto nextStep(GridSection[][] grid) {
        log.debug("Distribute walker id{}, status:{} next step...", id, status);
        log.debug("Current step: {}, total steps: {}, main path length:{}", currentStep, totalSteps, mainPathLength);
        if (status == Status.WAITING) {
            if (subWalkers.isEmpty()) {
                log.debug("No sub-walkers present, finishing walker...");
                status = Status.FINISHED;
            }
            return null;
        }
        val adjacentSections = getAdjacentSectionsInCluster(currentSection.getPoint(), grid, cluster);
        if (nonNull(previousRoom)) {
            adjacentSections.removeIf(section -> section.getPoint().equals(previousRoom.getPoint()));
        }
        GridSection nextSection = null;
        if (adjacentSections.size() > 1 && !runSubWalkerOnRouteFork) {
            log.debug("Running sub-walker on route fork disabled...");
            if (subWalkers.isEmpty()) {
                log.debug("Finishing walker...");
                status = Status.FINISHED;
                return null;
            }
        } else {
            log.debug("Assigning next section...");
            Optional<GridSection> nextSectionOptional = adjacentSections.stream().max(Comparator.comparing(GridSection::getStepsFromStart));
            if (nextSectionOptional.isPresent()) {
                nextSection = nextSectionOptional.get();
                log.debug("Next section: {}", nextSection);
            } else {
                if (subWalkers.isEmpty()) {
                    log.debug("Finishing walker...");
                    status = Status.FINISHED;
                } else {
                    log.debug("Running sub-walkers remaining: {}, switching to waiting mode...", subWalkers.size());
                    status = Status.WAITING;
                }
                return null;
            }
        }


        if (nonNull(nextSection)) {
            currentStep--;
            var room = buildRoom(nextSection, chatId);
            if (nonNull(previousRoom)) {
                setMutualAdjacency(room, previousRoom);
            }
            if (runSubWalkerOnRouteFork && adjacentSections.size() > 1) {
                log.debug("Processing route fork...");
                log.debug("Running sub-walker on route fork...");
                adjacentSections.stream().filter(section -> section.getStepsFromStart() == currentStep - 1).forEach(section -> subWalkers
                        .add(WalkerDistributor.builder()
                                .chatId(chatId)
                                .cluster(cluster)
                                .runSubWalkerOnRouteFork(false)
                                .status(WalkerDistributor.Status.RUNNING)
                                .currentSection(section)
                                .previousRoom(room)
                                .currentStep(totalSteps - mainPathLength)
                                .totalSteps(totalSteps - mainPathLength)
                                .build()));
                log.debug("Sub-walkers: {}", subWalkers);

                if (currentStep == 0) {
                    if (!subWalkers.isEmpty()) {
                        status = Status.WAITING;
                    } else {
                        status = Status.FINISHED;
                    }
                    return null;
                }
                currentSection = nextSection;
                previousRoom = room;
                return NextRoomDto.builder()
                        .room(room)
                        .section(nextSection)
                        .currentStep(currentStep + 1)
                        .totalSteps(runSubWalkerOnRouteFork ? totalSteps++ : totalSteps)
                        .build();
            } else {
                if (subWalkers.isEmpty()) {
                    status = Status.FINISHED;
                } else {
                    status = Status.WAITING;
                }
                return null;
            }
        }
        if (subWalkers.isEmpty()) {
            status = Status.FINISHED;
        } else {
            status = Status.WAITING;
        }
        return null;
    }

    private Room buildRoom(GridSection section, Long chatId) {
        val room = new Room();
        room.setChatId(chatId);
        room.setPoint(section.getPoint());
        return room;
    }

    private void setMutualAdjacency(Room room, Room previousRoom) {
        var direction = getDirection(room, previousRoom);
        if (nonNull(direction)) {//TODO: investigate NPE
            previousRoom.getAdjacentRooms().put(direction, true);
            room.getAdjacentRooms().put(getOppositeDirection(direction), true);
        }
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
        return this.id.equals(walkerDistributor.getId()) &&
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
