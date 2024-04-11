package org.dungeon.prototype.model.ui.level;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.Point;

import java.util.Optional;

import static org.dungeon.prototype.util.LevelUtil.getIcon;

@Data
@NoArgsConstructor
public class GridSection {
    private Boolean visited;
    private Boolean deadEnd;
    private Integer stepsFromStart;
    private Point coordinates;
    private String emoji;

    public GridSection(Point coordinates) {
        this.coordinates = coordinates;
        this.stepsFromStart = 0;
        this.visited = false;
    }

    public GridSection(Integer x, Integer y) {
        this.coordinates = Point.of(x, y);
        this.stepsFromStart = 0;
        this.visited = false;
        this.emoji = getIcon(Optional.empty());
    }
}
