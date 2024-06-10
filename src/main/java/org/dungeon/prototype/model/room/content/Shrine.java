package org.dungeon.prototype.model.room.content;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.dungeon.prototype.model.room.RoomContent;


@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HealthShrine.class, name = "HealthShrine"),
        @JsonSubTypes.Type(value = ManaShrine.class, name = "ManaShrine")
})
public abstract class Shrine implements RoomContent {

}
