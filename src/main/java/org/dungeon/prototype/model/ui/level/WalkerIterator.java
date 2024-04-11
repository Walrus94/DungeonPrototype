package org.dungeon.prototype.model.ui.level;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Room;

@Setter
@Getter
@AllArgsConstructor(staticName = "of")
public class WalkerIterator {
    private GridSection currentPoint;
    private Level.Direction previousDirection;
    private Room previousRoom;
    public WalkerIterator(WalkerIterator walkerIterator) {
        this.currentPoint = walkerIterator.currentPoint;
        this.previousDirection = walkerIterator.previousDirection;
        this.previousRoom = walkerIterator.previousRoom;
    }

    @Override
    public String toString() {
        return "currentPoint=" + currentPoint +
                ", previousDirection=" + previousDirection +
                ", previousRoom=" + previousRoom;
    }
}
