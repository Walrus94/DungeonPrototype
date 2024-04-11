package org.dungeon.prototype.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder(toBuilder = true)
public class Room {
    public enum Type {
        NORMAL, START, END, MONSTER, TREASURE
    }
    private Room left;
    private Room middle;
    private Room right;
    private Room entrance;
    @Builder.Default
    private Type type = Type.NORMAL;

    private Point point;

    @Override
    public String toString() {
        return "Room [type=" + type + ", point=" + point + "]";
    }
}
