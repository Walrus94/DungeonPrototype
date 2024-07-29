package org.dungeon.prototype.repository;

import org.dungeon.prototype.model.document.room.RoomContentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoomContentRepository extends MongoRepository<RoomContentDocument, String> {
}
