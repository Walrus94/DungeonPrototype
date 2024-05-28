package org.dungeon.prototype.service.level;

import lombok.Builder;
import lombok.Data;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.service.WalkerUniqueIdFactory;
import org.dungeon.prototype.util.LevelUtil;

import java.util.Objects;

@Data
@Builder
public class WalkerBuilderIterator {
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
        if (!(obj instanceof WalkerBuilderIterator walkerBuilderIterator)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return this.id.equals(walkerBuilderIterator.getId()) &&
                this.currentPoint.equals(walkerBuilderIterator.getCurrentPoint()) &&
                this.direction.equals(walkerBuilderIterator.getDirection()) &&
                this.previousRoom.equals(walkerBuilderIterator.getPreviousRoom());
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
