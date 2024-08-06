package org.dungeon.prototype.repository;

import org.dungeon.prototype.model.document.item.EffectDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EffectRepository extends MongoRepository<EffectDocument, String> {

}
