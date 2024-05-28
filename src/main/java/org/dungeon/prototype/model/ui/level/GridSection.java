package org.dungeon.prototype.model.ui.level;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.Point;

import java.util.Optional;

import static org.dungeon.prototype.util.LevelUtil.getIcon;

@Data
@NoArgsConstructor
public class GridSection {
    private Boolean visited;
    private Boolean deadEnd = false;
    private Boolean crossroad = false;
    private Integer stepsFromStart;
    private Point point;
    private String emoji;

    public GridSection(Integer x, Integer y) {
        this.point = Point.of(x, y);
        this.stepsFromStart = 0;
        this.visited = false;
        this.crossroad = false;
        this.emoji = getIcon(Optional.empty());
    }
}
