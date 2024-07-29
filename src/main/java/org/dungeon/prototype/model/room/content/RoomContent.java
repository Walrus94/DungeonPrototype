package org.dungeon.prototype.model.room.content;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.dungeon.prototype.model.room.RoomType;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EmptyRoom.class, name = "EmptyRoom"),
        @JsonSubTypes.Type(value = StartRoom.class, name = "StartRoom"),
        @JsonSubTypes.Type(value = EndRoom.class, name = "EndRoom"),
        @JsonSubTypes.Type(value = MonsterRoom.class, name = "MonsterRoom"),
        @JsonSubTypes.Type(value = Treasure.class, name = "Treasure"),
        @JsonSubTypes.Type(value = Merchant.class, name = "Merchant"),
        @JsonSubTypes.Type(value = NormalRoom.class, name = "NormalRoom"),
        @JsonSubTypes.Type(value = HealthShrine.class, name = "HealthShrine"),
        @JsonSubTypes.Type(value = ManaShrine.class, name = "ManaShrine")
})
public interface RoomContent {
    String getId();
    Integer getRoomContentWeight();
    RoomType getRoomType();
}
