package org.dungeon.prototype.service.room.generation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.monster.MonsterAttack;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.HealthShrine;
import org.dungeon.prototype.model.room.content.ManaShrine;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.model.room.content.NormalRoom;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.properties.GenerationProperties;
import org.dungeon.prototype.repository.MonsterRepository;
import org.dungeon.prototype.repository.converters.mapstruct.MonsterMapper;
import org.dungeon.prototype.service.item.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

import static org.dungeon.prototype.util.RandomUtil.getNormalDistributionRandomDouble;
import static org.dungeon.prototype.util.RandomUtil.getRandomInt;
import static org.dungeon.prototype.util.RoomGenerationUtils.convertToMonsterClass;


@Slf4j
@Component
public class RoomContentRandomFactory {
    private static final Integer MERCHANT_MAX_ITEMS = 6;
    private static final Integer MAX_TREASURE_ITEMS = 3; //TODO: consider configuring;

    @Autowired
    private GenerationProperties generationProperties;
    @Autowired
    private ItemService itemService;
    @Autowired
    private MonsterRoomGenerationService monsterRoomGenerationService;
    @Autowired
    private MonsterRepository monsterRepository;

    public RoomContent getNextRoomContent(LevelRoomTypeClusters clusters, RoomType roomType, Integer expectedWeightAbs) {
        val chatId = clusters.getChatId();
        val usedItemIds = clusters.getUsedItemIds();
        log.debug("Generating room content with type {}, expected weight: {}, used items ids: {}", roomType, expectedWeightAbs, usedItemIds);
        return switch (roomType) {
            case WEREWOLF, VAMPIRE, SWAMP_BEAST, DRAGON, ZOMBIE -> getMonster(expectedWeightAbs, roomType);
            case TREASURE -> getTreasure(chatId, expectedWeightAbs, usedItemIds);
            case MERCHANT -> getMerchant(chatId, expectedWeightAbs, usedItemIds);
            case HEALTH_SHRINE -> new HealthShrine();
            case MANA_SHRINE -> new ManaShrine();
            default -> new NormalRoom();
        };
    }

    private Merchant getMerchant(Long chatId, Integer expectedWeight, Set<String> usedItemIds) {
        log.debug("Generating Merchant...");
        val merchant = new Merchant();
        val items = itemService.getExpectedWeightItems(chatId, expectedWeight, MERCHANT_MAX_ITEMS, usedItemIds);
        merchant.setItems(items);
        return merchant;
    }

    private Treasure getTreasure(Long chatId, Integer expectedWeight, Set<String> usedItemIds) {
        log.debug("Generating treasure...");
        log.debug("Expected weight: {}", expectedWeight);
        int gold;
        val itemsCount = getRandomInt(0, MAX_TREASURE_ITEMS);
        log.debug("Treasure items amount: {}", itemsCount);
        Set<Item> items = itemsCount > 0 ? itemService.getExpectedWeightItems(chatId, expectedWeight, itemsCount, usedItemIds) : Collections.emptySet();
        if (!items.isEmpty()) {
            if (expectedWeight > items.stream().mapToInt(Item::getWeight).sum()) {
                gold = getRandomGoldAmount((double) (expectedWeight - items.stream().mapToInt(Item::getWeight).sum()));
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
            gold = getRandomGoldAmount(expectedWeight.doubleValue());
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

    private MonsterRoom getMonster(Integer expectedWeight, RoomType roomType) {
        val monster = generateNextMonster(expectedWeight, roomType);
        val monsterRoom = new MonsterRoom();
        monsterRoom.setMonster(monster);
        return monsterRoom;
    }

    private Monster generateNextMonster(Integer weight, RoomType roomType) {
        log.debug("Generating monster of type {} and weight {}", roomType, weight);
        val monsterClass = convertToMonsterClass(roomType);
        val properties = generationProperties.getMonsters().get(monsterClass);
        Integer level = monsterRoomGenerationService.getMonsterClassLevelByWeight(monsterClass, weight);
        log.debug("Generating {} level {}", monsterClass, level);
        val monster = new Monster();
        monster.setMonsterClass(monsterClass);
        monster.setLevel(level);
        monster.setMaxHp(getRandomInt(properties.getHealthRatio() * level, (properties.getHealthRatio() * level) + properties.getHealthBonus()));
        monster.setHp(monster.getMaxHp());
        monster.setPrimaryAttack(MonsterAttack.of(properties.getPrimaryAttackType(),
                getRandomInt(properties.getPrimaryAttackRatio() * level, (properties.getPrimaryAttackRatio() * level) + properties.getPrimaryAttackBonus())));
        monster.setSecondaryAttack(MonsterAttack.of(properties.getSecondaryAttackType(),
                getRandomInt(properties.getSecondaryAttackRatio() * level, properties.getSecondaryAttackRatio() * level + properties.getSecondaryAttackBonus())));
        monster.setXpReward(monster.getPrimaryAttack().getAttack() * properties.getWeightPrimaryAttackMultiplier() +
                monster.getSecondaryAttack().getAttack() * properties.getWeightSecondaryAttackMultiplier() +
                monster.getMaxHp());
        val monsterDocument = MonsterMapper.INSTANCE.mapToDocument(monster);
        return MonsterMapper.INSTANCE.mapToMonster(monsterRepository.save(monsterDocument));
    }
}
