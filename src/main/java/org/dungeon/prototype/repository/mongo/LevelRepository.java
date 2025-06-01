package org.dungeon.prototype.repository.mongo;


import org.dungeon.prototype.model.document.level.LevelDocument;
import org.dungeon.prototype.repository.mongo.projections.LevelNumberProjection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LevelRepository extends MongoRepository<LevelDocument, Long> {
    @Query(value = "{ '_id' : ?0 }")
    Optional<LevelDocument> findByChatId(@Param("_id") Long chatId);

    @Query(value = "{ '_id': ?0}", fields = "{ 'number': 1, '_id': 0 }")
    Optional<LevelNumberProjection> findNumberByChatId(@Param("_id") Long chatId);

    void removeByChatId(Long chatId);

    Boolean existsByChatId(Long chatId);
}
