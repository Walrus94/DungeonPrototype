package org.dungeon.prototype.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dungeon.prototype.util.LevelUtil;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class Player implements Serializable {
    @Serial
    private static final long serialVersionUID = 6523075017967757691L;
    private Point currentRoom;
    private LevelUtil.Direction direction;
    private Long xp;
    private Integer hp;
    private Integer mana;
}
