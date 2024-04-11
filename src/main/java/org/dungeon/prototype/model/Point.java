package org.dungeon.prototype.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class Point {
    private Integer x;
    private Integer y;
}
