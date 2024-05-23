package org.dungeon.prototype.model.ui.level;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.dungeon.prototype.model.Room;
import org.dungeon.prototype.util.LevelUtil;

@Setter
@Getter
@AllArgsConstructor(staticName = "of")
public class WalkerIterator {
    private GridSection currentPoint;
    private LevelUtil.Direction direction;
    private Room previousRoom;
    private int roomMonsters;
    private int roomTreasures;
    public WalkerIterator(WalkerIterator walkerIterator) {
        this.currentPoint = walkerIterator.getCurrentPoint();
        this.direction = walkerIterator.getDirection();
        this.previousRoom = walkerIterator.getPreviousRoom();
        this.roomTreasures = walkerIterator.getRoomTreasures();
        this.roomMonsters = walkerIterator.getRoomMonsters();
    }

    @Override
    public String toString() {
        return "currentPoint=" + currentPoint +
                ", direction=" + direction +
                ", previousRoom=" + previousRoom +
                ", roomMonsters=" + roomMonsters +
                ", roomTreasures=" + roomTreasures;
    }
}
