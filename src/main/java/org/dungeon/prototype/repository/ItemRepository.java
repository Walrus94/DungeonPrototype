package org.dungeon.prototype.repository;

import org.dungeon.prototype.model.document.item.ItemDocument;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.repository.projections.ItemWeightProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ItemRepository extends MongoRepository<ItemDocument, String> {

    @Query(value = "{'chatId': ?0, 'itemType': 'WEARABLE', 'attributes.wearableType': ?1}", sort = "{'weight':  1}")
    List<ItemDocument> findWearablesByChatIdTypeAndMinWeight(Long chatId, WearableType wearableType, Pageable pageable);

    @Query(value = "{'chatId': ?0, 'itemType' : 'WEAPON' }", sort = "{'weight':  1}")
    List<ItemDocument> findMainWeaponByChatIdAndMinWeight(Long chatId, Pageable pageable);

    @Query(value = "{ 'chatId':  ?0, 'weightAbs':  {$lt: ?1}, '_id' : {'$nin' : ?2}}", sort = "{ 'weightAbs': -1}")
    List<ItemWeightProjection> findClosestLesserWeight(Long chatId, double value, Set<String> usedItemIds, Pageable pageable);
    Optional<ItemDocument> findByChatIdAndId(Long chatId, String id);
    List<ItemDocument> findAllByChatIdAndIdIn(Long chatId, List<String> ids);
    void deleteAllByChatId(Long chatId);
}
