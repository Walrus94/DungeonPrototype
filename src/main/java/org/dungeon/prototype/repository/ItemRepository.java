package org.dungeon.prototype.repository;

import org.dungeon.prototype.model.document.item.ItemDocument;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.repository.projections.ItemWeightProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ItemRepository extends MongoRepository<ItemDocument, String> {

    @Query(value = "{'chatId': ?0, 'itemType': 'WEARABLE', 'attributes.wearableType': ?1}", sort = "{'weight':  1}")
    List<ItemDocument> findWearablesByChatIdTypeAndMinWeight(Long chatId, WearableType wearableType, Pageable pageable);

    @Query(value = "{'chatId': ?0, 'itemType' : 'WEAPON', 'attributes.weaponType' : {'$ne': 'ADDITIONAL'} }", sort = "{'weight':  1}")
    List<ItemDocument> findMainWeaponByChatIdAndMinWeight(Long chatId, Pageable pageable);

    List<ItemDocument> findByChatIdAndWeightAndIdNotIn(Long chatId, Integer weight, Set<String> ids, Pageable pageable);

    @Query(value = "{ 'chatId':  ?0, 'weight':  {$lt: ?1}, '_id' : {'$nin' : ?2}}", sort = "{ 'weight': -1}")
    List<ItemWeightProjection> findClosestLesserWeight(Long chatId, int value, Set<String> usedItemIds, Pageable pageable);
    @Query(value = "{ 'chatId': ?0, 'weight': { $lte: ?1 } }", sort = "{ 'weight': -1}")
    List<ItemDocument> findClosestLesserOrEqual(Long chatId, int value, Pageable pageable);

    @Query(value = "{ 'chatId':  ?0, 'weight':  {$gt: ?1}, '_id' : {'$nin' : ?2}}", sort = "{ 'weight': 1}")
    List<ItemWeightProjection> findClosestGreaterWeight(Long chatId, int value, Set<String> usedItemIds, Pageable pageable);

    @Query(value = "{ 'chatId': ?0, 'weight': { $gte: ?1 } }", sort = "{ 'weight': 1}")
    List<ItemDocument> findClosestGreaterOrEqual(Long chatId, int value, Pageable pageable);

    @Query(value = "{ 'chatId': ?0 }", sort = "{ 'weight' : 1 }")
    List<ItemWeightProjection> findFirstByOrderByWeightAsc(Long chatId, Pageable pageable);

    @Query(value = "{ 'chatId': ?0 }", sort = "{ 'weight' : -1 }")
    List<ItemWeightProjection> findFirstByOrderByWeightDesc(Long chatId, Pageable pageable);

    void deleteAllByChatId(Long chatId);
}
