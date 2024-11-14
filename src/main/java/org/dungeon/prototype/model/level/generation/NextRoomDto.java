package org.dungeon.prototype.model.level.generation;

import lombok.Builder;
import lombok.Data;
import org.dungeon.prototype.model.level.ui.GridSection;
import org.dungeon.prototype.model.room.Room;

@Data
@Builder
public class NextRoomDto {
    private Room room;
    private GridSection section;
    private int currentStep;
    private int totalSteps;
}
