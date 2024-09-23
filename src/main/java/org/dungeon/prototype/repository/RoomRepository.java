package org.dungeon.prototype.repository;

import org.dungeon.prototype.model.document.room.RoomDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends MongoRepository<RoomDocument, String> {

    @Query(value = "{'chatId' : ?0, '_id': ?1}")
    Optional<RoomDocument> findByChatIdAndId(@Param("chatId") Long chatId, @Param("_id") String roomId);
}
