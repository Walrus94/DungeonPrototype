package org.dungeon.prototype.repository.mongo;

import org.dungeon.prototype.model.document.balance.BalanceMatrixDocument;
import org.dungeon.prototype.repository.mongo.projections.MatrixValueProjection;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BalanceMatrixRepository extends MongoRepository<BalanceMatrixDocument, String> {
    boolean existsByChatIdAndName(long chatId, String name);
    Optional<BalanceMatrixDocument> findByChatIdAndName(long chatId, String name);
    @Aggregation(pipeline = {
            "{ '$match': { 'chatId': ?0, 'name': ?1 } }",
            "{ '$project': { 'data': { '$arrayElemAt': ['$data', ?2] } } }",
            "{ '$project': { 'value': { '$arrayElemAt': ['$data', ?3] } } }"
    })
    Optional<MatrixValueProjection> getMatrixCellValue(Long chatId, String name, int row, int col);
    void deleteAllByChatId(long chatId);
}
