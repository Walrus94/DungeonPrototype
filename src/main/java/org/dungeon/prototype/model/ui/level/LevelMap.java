package org.dungeon.prototype.model.ui.level;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
public class LevelMap {
    private Set<GridSection> visitedRooms = new HashSet<>();
    private Integer maxX;
    private Integer minX;
    private Integer minY;
    private Integer maxY;

    public LevelMap(GridSection start) {
        visitedRooms.add(start);
        maxX = start.getPoint().getX() + 1;
        minX = start.getPoint().getX() - 1;
        maxY = start.getPoint().getY() + 1;
        minY = start.getPoint().getY() - 1;
    }

    public boolean addRoom(GridSection section) {
        if (visitedRooms.add(section)) {
            if (section.getPoint().getX() + 1 > maxX) {
                maxX = section.getPoint().getX() + 1;
            }
            if (section.getPoint().getX() - 1 < minX) {
                minX = section.getPoint().getX() - 1;
            }
            if (section.getPoint().getY() + 1 > maxY) {
                maxY = section.getPoint().getY() + 1;
            }
            if (section.getPoint().getY() - 1 < minY) {
                minY = section.getPoint().getY() - 1;
            }
            return true;
        }
        return false;
    }

    public boolean isContainsRoom(Integer x, Integer y) {
        return visitedRooms.stream()
                .anyMatch(section -> section.getPoint().getX().equals(x) &&
                        section.getPoint().getY().equals(y));
    }
}
