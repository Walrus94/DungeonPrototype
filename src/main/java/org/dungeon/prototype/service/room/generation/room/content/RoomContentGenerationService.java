package org.dungeon.prototype.service.room.generation.room.content;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.effect.ExpirableAdditionEffect;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.HealthShrine;
import org.dungeon.prototype.model.room.content.ManaShrine;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.model.room.content.NormalRoom;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.repository.MonsterRepository;
import org.dungeon.prototype.repository.converters.mapstruct.MonsterMapper;
import org.dungeon.prototype.service.effect.EffectFactory;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.room.generation.LevelRoomTypeClusters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.HEALTH;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.MANA;
import static org.dungeon.prototype.util.RandomUtil.getNormalDistributionRandomDouble;
import static org.dungeon.prototype.util.RandomUtil.getRandomInt;
import static org.dungeon.prototype.util.RandomUtil.getRandomRoomType;
import static org.dungeon.prototype.util.RoomGenerationUtils.convertToMonsterClass;


@Slf4j
@Service
public class RoomContentGenerationService {
    private static final Integer MERCHANT_MAX_ITEMS = 6;
    private static final Integer MAX_TREASURE_ITEMS = 3; //TODO: consider configuring;

    @Autowired
    private ItemService itemService;
    @Autowired
    private MonsterFactory roomContentFactory;
    @Autowired
    private EffectFactory effectFactory;
    @Autowired
    private MonsterRepository monsterRepository;

    public RoomContent getNextRoomContent(LevelRoomTypeClusters clusters, Weight expectedWeight) {
        val chatId = clusters.getChatId();
        val usedItemIds = clusters.getUsedItemIds();
        val roomsLeft = clusters.getRoomsLeft();
        val totalRooms = clusters.getTotalRooms();
        log.debug("Total rooms: {}, rooms left: {}", totalRooms, roomsLeft);
        log.debug("Generating next room type...");
        log.debug("Generating random room type...");
        val currentStep = totalRooms - roomsLeft;//TODO verify with debugger
        RoomType roomType;
        if (clusters.isHasMerchantRoom()) {//TODO consider limits or predicates for other room types
            roomType = getRandomRoomType(expectedWeight, currentStep, totalRooms, RoomType.MERCHANT);
        } else {
            roomType = getRandomRoomType(expectedWeight, currentStep, totalRooms);
        }
        log.debug("Random room type: {}", roomType);
        log.debug("Generating room content with type {}, expected weight: {}, used items ids: {}", roomType, expectedWeight, usedItemIds);
        return switch (roomType) {
            case WEREWOLF, VAMPIRE, SWAMP_BEAST, DRAGON, ZOMBIE -> getMonster(expectedWeight, roomType);
            case TREASURE -> getTreasure(chatId, expectedWeight, usedItemIds);
            case MERCHANT -> {
                clusters.setHasMerchantRoom(true);
                yield getMerchant(chatId, expectedWeight, usedItemIds);
            }
            case HEALTH_SHRINE -> getHealthShrine(expectedWeight.toVector().getNorm());
            case MANA_SHRINE -> getManaShrine(expectedWeight.toVector().getNorm());
            default -> new NormalRoom();
        };
    }

    private ManaShrine getManaShrine(double expectedWeightAbs) {
        val manaShrine = new ManaShrine();
        ExpirableAdditionEffect manaRegeneration = effectFactory.generateRegenerationEffect(MANA, expectedWeightAbs);
        manaShrine.setEffect(manaRegeneration);
        return manaShrine;
    }

    private HealthShrine getHealthShrine(double expectedWeightAbs) {
        val healthShrine = new HealthShrine();
        val regeneration = effectFactory.generateRegenerationEffect(HEALTH, expectedWeightAbs);
        healthShrine.setEffect(regeneration);
        return healthShrine;
    }

    private Merchant getMerchant(Long chatId, Weight expectedWeight, Set<String> usedItemIds) {
        log.debug("Generating Merchant...");
        val merchant = new Merchant();
        val items = itemService.getExpectedWeightItems(chatId, expectedWeight, MERCHANT_MAX_ITEMS, usedItemIds);
        merchant.setItems(items);
        return merchant;
    }

    private Treasure getTreasure(Long chatId, Weight expectedWeight, Set<String> usedItemIds) {
        log.debug("Generating treasure...");
        log.debug("Expected weight: {}", expectedWeight);
        int gold;
        val itemsCount = getRandomInt(0, MAX_TREASURE_ITEMS);
        log.debug("Treasure items amount: {}", itemsCount);
        Set<Item> items = itemsCount > 0 ? itemService.getExpectedWeightItems(chatId, expectedWeight, itemsCount, usedItemIds) : Collections.emptySet();
        if (!items.isEmpty()) {
            if (expectedWeight.toVector().getNorm() > items.stream().map(Item::getWeight).reduce(Weight::add).orElse(new Weight()).toVector().getNorm()) {
                gold = getRandomGoldAmount(expectedWeight.toVector().getNorm() - items.stream().map(Item::getWeight).reduce(Weight::add).orElse(new Weight()).toVector().getNorm());
            } else {
                gold = 0;
            }
            log.debug("Gold: {}", gold);
            log.debug("Items: {}", items);
            val treasure = new Treasure();
            treasure.setGold(gold);
            treasure.setItems(items);
            return treasure;
        } else {
            gold = getRandomGoldAmount(expectedWeight.toVector().getNorm());
            log.debug("Gold: {}", gold);
            log.debug("No treasures");
            val treasure = new Treasure();
            treasure.setGold(gold);
            treasure.setItems(items);
            return treasure;
        }
    }

    private static int getRandomGoldAmount(Double expectedWeight) {
        return getNormalDistributionRandomDouble(expectedWeight,
                expectedWeight * 0.1).intValue();
    }

    private MonsterRoom getMonster(Weight expectedWeight, RoomType roomType) {
        val monster = generateNextMonster(expectedWeight, roomType);
        val monsterRoom = new MonsterRoom();
        monsterRoom.setMonster(monster);
        return monsterRoom;
    }

    private Monster generateNextMonster(Weight weight, RoomType roomType) {
        log.debug("Generating monster of type {} and weight {}", roomType, weight);
        val monsterClass = convertToMonsterClass(roomType);

        Monster monster = roomContentFactory.generateMonsterByExpectedWeight(weight, monsterClass);
        val monsterDocument = MonsterMapper.INSTANCE.mapToDocument(monster);
        return MonsterMapper.INSTANCE.mapToMonster(monsterRepository.save(monsterDocument));
    }
}
