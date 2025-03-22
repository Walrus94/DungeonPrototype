package org.dungeon.prototype.repository.mongo;

import org.dungeon.prototype.model.document.monster.MonsterDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonsterRepository extends MongoRepository<MonsterDocument, String> {
}
