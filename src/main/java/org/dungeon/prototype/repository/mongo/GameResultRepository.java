package org.dungeon.prototype.repository.mongo;

import org.dungeon.prototype.model.document.stats.GameResultDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GameResultRepository extends MongoRepository<GameResultDocument, String> {
}
