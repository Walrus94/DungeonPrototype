package org.dungeon.prototype.repository;

import org.dungeon.prototype.model.document.monster.MonsterDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonsterRepository extends MongoRepository<MonsterDocument, String> {
    @Query(value = "{ 'chatId' : ?0 }")
    Optional<MonsterDocument> findByChatId(@Param("chatId") Long chatId);
}
