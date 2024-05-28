package org.dungeon.prototype.service.room;

import lombok.Builder;
import lombok.Data;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.service.WalkerUniqueIdFactory;

import java.util.Objects;

@Data
@Builder
public class WalkerDistributeIterator {
    @Builder.Default
    private Long id = WalkerUniqueIdFactory.getInstance().getNextId();
    private Room currentRoom;
    private Room previousRoom;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WalkerDistributeIterator walkerDistributeIterator)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return this.id.equals(walkerDistributeIterator.getId()) &&
                this.currentRoom.equals(walkerDistributeIterator.getCurrentRoom()) &&
                this.previousRoom.equals(walkerDistributeIterator.getPreviousRoom());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, currentRoom, previousRoom);
    }
}
