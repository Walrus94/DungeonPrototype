package org.dungeon.prototype.model.room;

import lombok.Getter;
import lombok.Setter;
import org.dungeon.prototype.model.ui.level.GridSection;

public class RoomsSegment {

    public RoomsSegment(GridSection start) {
        this.start = start;
    }

    public RoomsSegment(GridSection start, GridSection end) {
        this.start = start;
        this.end = end;
    }

    @Getter
    private final GridSection start;
    @Getter
    @Setter
    private GridSection end;

    @Override
    public String toString() {
        return "start: " + start + ", end: " + end;
    }
}
