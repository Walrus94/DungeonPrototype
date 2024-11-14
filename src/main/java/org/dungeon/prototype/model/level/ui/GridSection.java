package org.dungeon.prototype.model.level.ui;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.Point;

import java.util.Optional;

import static org.dungeon.prototype.util.LevelUtil.getIcon;

@Data
@NoArgsConstructor
public class GridSection {
    private boolean deadEnd;
    private boolean connectionPoint;
    private int stepsFromStart;
    private Point point;
    private String emoji;

    public GridSection(Integer x, Integer y) {
        this.point = new Point(x, y);
        this.stepsFromStart = 0;
        this.connectionPoint = false;
        this.deadEnd = false;
        this.emoji = getIcon(Optional.empty());
    }
}
