package org.dungeon.prototype.repository;

import org.dungeon.prototype.model.document.player.ArmorSetDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArmorSetRepository extends MongoRepository<ArmorSetDocument, String> {
}
