package org.dungeon.prototype.repository;

import org.dungeon.prototype.model.document.player.PlayerDocument;
import org.dungeon.prototype.repository.projections.NicknameProjection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface PlayerRepository extends MongoRepository<PlayerDocument, Long> {
    @Query(value = "{ 'chatId' : ?0 }")
    Optional<PlayerDocument> findByChatId(@Param("chatId") Long chatId);

    Boolean existsByChatId(Long chatId);

    @Query(value = "{ 'chatId' : ?0 }", fields = "{ 'nickname' : 1, '_id' : 0 }")
    Optional<NicknameProjection> getNicknameByChatId(@Param("_id") Long chatId);
}
