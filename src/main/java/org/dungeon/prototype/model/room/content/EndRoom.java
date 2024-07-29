package org.dungeon.prototype.model.room.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dungeon.prototype.model.room.RoomType;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class EndRoom extends NoContentRoom {
    @Override
    public RoomType getRoomType() {
        return RoomType.END;
    }
}
