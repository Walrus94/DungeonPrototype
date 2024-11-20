package org.dungeon.prototype.service.room.generation.room.content;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.effect.ExpirableAdditionEffect;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.*;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.repository.MonsterRepository;
import org.dungeon.prototype.repository.converters.mapstruct.MonsterMapper;
import org.dungeon.prototype.service.effect.EffectFactory;
import org.dungeon.prototype.service.item.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.HEALTH;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.MANA;
import static org.dungeon.prototype.util.RandomUtil.getNormalDistributionRandomDouble;
import static org.dungeon.prototype.util.RandomUtil.getRandomInt;
import static org.dungeon.prototype.util.RoomGenerationUtils.convertToMonsterClass;


@Slf4j
@Service
public class RoomContentGenerationService {
    private static final Integer MERCHANT_MAX_ITEMS = 6;
    private static final Integer MAX_TREASURE_ITEMS = 3; //TODO: consider configuring;

    @Autowired
    private ItemService itemService;
    @Autowired
    private MonsterFactory monsterFactory;
    @Autowired
    private EffectFactory effectFactory;
    @Autowired
    private AnvilFactory anvilFactory;
    @Autowired
    private MonsterRepository monsterRepository;

    /**
     * Generates random room content of expected weight
     *
     * @param expectedWeight of generated content
     * @param chatId of player
     * @param roomType expected type
     * @param usedItemIds items already used on the level
     * @return generated room content
     */
    public RoomContent getNextRoomContent(Weight expectedWeight, long chatId, RoomType roomType, Set<String> usedItemIds) {
        log.info("Generating next room: {}...", roomType);

        if (expectedWeight.toVector().getNorm() == 0.0) {//TODO: consider configuring threshold
            log.info("Weight is below threshold, generating normal room");
            return new NormalRoom();
        }
        log.info("Generating {} of expected weight: {}", roomType, expectedWeight);

        log.info("Generating room content with type {}, expected weight: {}, used items ids: {}", roomType, expectedWeight, usedItemIds);
        return switch (roomType) {
            case WEREWOLF, VAMPIRE, SWAMP_BEAST, DRAGON, ZOMBIE -> getMonster(expectedWeight, roomType);
            case TREASURE -> getTreasure(chatId, expectedWeight, usedItemIds);
            case MERCHANT -> getMerchant(chatId, expectedWeight, usedItemIds);
            case ANVIL -> getAnvil(expectedWeight);
            case HEALTH_SHRINE -> getHealthShrine(expectedWeight.toVector().getNorm());
            case MANA_SHRINE -> getManaShrine(expectedWeight.toVector().getNorm());
            default -> new NormalRoom();
        };
    }

    private Anvil getAnvil(Weight expectedWeight) {
        return anvilFactory.generateAnvil(expectedWeight);
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
        log.info("Generating Merchant...");
        val merchant = new Merchant();
        val items = itemService.getExpectedWeightItems(chatId, expectedWeight, MERCHANT_MAX_ITEMS, usedItemIds);
        merchant.setItems(items);
        return merchant;
    }

    private Treasure getTreasure(long chatId, Weight expectedWeight, Set<String> usedItemIds) {
        log.info("Generating treasure...");
        log.info("Expected weight: {}", expectedWeight);
        int gold;
        val itemsCount = getRandomInt(0, MAX_TREASURE_ITEMS);
        log.info("Treasure items amount: {}", itemsCount);
        Set<Item> items = itemsCount > 0 ? itemService.getExpectedWeightItems(chatId, expectedWeight, itemsCount, usedItemIds) : Collections.emptySet();
        if (!items.isEmpty()) {
            if (expectedWeight.toVector().getNorm() > items.stream().map(Item::getWeight).reduce(Weight::add).orElse(new Weight()).toVector().getNorm()) {
                gold = getRandomGoldAmount(expectedWeight.toVector().getNorm() - items.stream().map(Item::getWeight).reduce(Weight::add).orElse(new Weight()).toVector().getNorm());
            } else {
                gold = 0;
            }
            log.info("Gold: {}", gold);
            log.info("Items: {}", items);
            val treasure = new Treasure();
            treasure.setGold(gold);
            treasure.setItems(items);
            return treasure;
        } else {
            gold = getRandomGoldAmount(expectedWeight.toVector().getNorm());
            log.info("Gold: {}", gold);
            log.info("No treasures");
            val treasure = new Treasure();
            treasure.setGold(gold);
            treasure.setItems(items);
            return treasure;
        }
    }

    public Treasure getSpecialTreasure(long chatId, int playerLuck, Set<String> usedItemIds) {
        Treasure treasure = new Treasure();
        treasure.setGold(0);
        treasure.setItems(Set.of(itemService.getHighQualityItem(chatId, playerLuck, usedItemIds)));
        return treasure;
    }

    private static int getRandomGoldAmount(Double expectedWeight) {
        return getNormalDistributionRandomDouble(expectedWeight,
                expectedWeight * 0.1).intValue();
    }

    private MonsterRoom getMonster(Weight expectedWeight, RoomType roomType) {
        val monsterClass = convertToMonsterClass(roomType);
        log.info("Generating monster of type {} and weight {}", roomType, expectedWeight);
        Monster monster = monsterFactory.generateMonsterByExpectedWeight(expectedWeight, monsterClass);
        val monsterRoom = new MonsterRoom();
        val monsterDocument = MonsterMapper.INSTANCE.mapToDocument(monster);
        monster = MonsterMapper.INSTANCE.mapToMonster(monsterRepository.save(monsterDocument));

        monsterRoom.setMonster(monster);
        return monsterRoom;
    }


}
