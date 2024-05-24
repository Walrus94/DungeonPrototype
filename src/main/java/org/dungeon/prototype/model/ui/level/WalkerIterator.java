package org.dungeon.prototype.model.ui.level;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.dungeon.prototype.model.Room;
import org.dungeon.prototype.util.LevelUtil;

import java.util.Objects;

@Setter
@Getter
@Builder
public class WalkerIterator {
    @Builder.Default
    private Long id = WalkerUniqueIdFactory.getInstance().getNextId();
    private GridSection currentPoint;
    private LevelUtil.Direction direction;
    private Room previousRoom;

    public Integer getPathFromStart() {
        return currentPoint.getStepsFromStart();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WalkerIterator walkerIterator)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return this.id.equals(walkerIterator.getId()) &&
                this.currentPoint.equals(walkerIterator.getCurrentPoint()) &&
                this.direction.equals(walkerIterator.getDirection()) &&
                this.previousRoom.equals(walkerIterator.getPreviousRoom());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, currentPoint, direction, previousRoom);
    }

    @Override
    public String toString() {
        return "id=" + id +
                ", currentPoint=" + currentPoint +
                ", direction=" + direction +
                ", previousRoom=" + previousRoom;
    }
}
