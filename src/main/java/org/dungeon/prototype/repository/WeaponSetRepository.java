package org.dungeon.prototype.repository;

import org.dungeon.prototype.model.document.player.WeaponSetDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeaponSetRepository extends MongoRepository<WeaponSetDocument, String> {
}
