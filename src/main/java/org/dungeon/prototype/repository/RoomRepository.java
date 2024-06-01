package org.dungeon.prototype.repository;

import org.dungeon.prototype.model.room.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends MongoRepository<Room, String> {

    @Query(value = "{'chatId' : ?0, '_id': ?1}")
    Room findByChatIdAndId(@Param("chatId") Long chatId, @Param("_id") String roomId);
}
