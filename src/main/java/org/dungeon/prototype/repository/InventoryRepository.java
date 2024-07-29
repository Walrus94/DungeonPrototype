package org.dungeon.prototype.repository;

import org.dungeon.prototype.model.document.player.InventoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends MongoRepository<InventoryDocument, String> {
}
