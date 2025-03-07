package org.dungeon.prototype.service.item;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.exception.EntityNotFoundException;
import org.dungeon.prototype.model.document.item.ItemDocument;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.repository.mongo.ItemRepository;
import org.dungeon.prototype.repository.mongo.converters.mapstruct.ItemMapper;
import org.dungeon.prototype.repository.mongo.projections.ItemWeightProjection;
import org.dungeon.prototype.service.item.generation.ItemNamingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.math3.util.FastMath.abs;

@Slf4j
@Component
public class ItemService {
    private final ItemMapper itemMapper = ItemMapper.INSTANCE;
    @Autowired
    private ItemNamingService itemNamingService;
    @Autowired
    private ItemRepository itemRepository;


    /**
     * Saves item to repository
     *
     * @param item to save
     * @return saved item
     */
    public Item saveItem(Item item) {
        val itemDocument = ItemMapper.INSTANCE.mapToDocument(item);
        val savedItemDocument = itemRepository.save(itemDocument);
        return switch (savedItemDocument.getItemType()) {
            case WEAPON -> ItemMapper.INSTANCE.mapToWeapon(savedItemDocument);
            case WEARABLE -> ItemMapper.INSTANCE.mapToWearable(savedItemDocument);
            case USABLE -> ItemMapper.INSTANCE.mapToUsable(savedItemDocument);
        };
    }

    /**
     * Looks for wearable item of given type and minimum weight
     * throws {@link EntityNotFoundException} if fails to find any
     *
     * @param chatId       current chat id
     * @param wearableType to look for
     * @return found item
     */
    public Wearable getMostLightweightWearable(Long chatId, WearableType wearableType) {
        List<ItemDocument> documents = itemRepository.findWearablesByChatIdTypeAndMinWeight(chatId, wearableType, PageRequest.of(0, 1));
        if (isNull(documents) || documents.isEmpty()) {
            log.error("Unable find most lightweight wearable of type {} for chat id {}!", wearableType, chatId);
            throw new EntityNotFoundException(chatId, wearableType.toString(), CallbackType.MENU_BACK);
        }

        //TODO: refactor
        var document = documents.getFirst();
        if (nonNull(document) && isNull(document.getName())) {
            itemNamingService.requestNameGeneration(itemMapper.mapToWearable(document));
            log.info("Waiting for name generation of item {} for chat {}...", document.getId(), chatId);
            while (isNull(document.getName())) {
                document = itemRepository.findByChatIdAndId(document.getChatId(), document.getId()).orElse(null);
            }
            log.info("Name for item id:{} acquired: {}", document.getId(), document.getName());
        }
        return itemMapper.mapToWearable(document);
    }

    /**
     * Looks for weapon of minimum weight
     * throws {@link EntityNotFoundException} if fails to find any
     *
     * @param chatId current chat id
     * @return found item
     */
    public Weapon getMostLightWeightMainWeapon(Long chatId) {
        val documents = itemRepository.findMainWeaponByChatIdAndMinWeight(chatId, PageRequest.of(0, 1));
        if (isNull(documents) || documents.isEmpty()) {
            log.error("Unable to find most lightweight weapon for chat id {}!", chatId);
            throw new EntityNotFoundException(chatId, "weapon", CallbackType.MENU_BACK);
        }

        //TODO: refactor
        var document = documents.getFirst();
        if (nonNull(document) && isNull(document.getName())) {
            itemNamingService.requestNameGeneration(itemMapper.mapToWeapon(document));
            log.info("Waiting for name generation of item {} for chat {}...", document.getId(), chatId);
            while (isNull(document.getName())) {
                document = itemRepository.findByChatIdAndId(document.getChatId(), document.getId()).orElse(null);
            }
            log.info("Name for item id:{} acquired: {}", document.getId(), document.getName());
        }
        return itemMapper.mapToWeapon(document);
    }

    public Item getHighQualityItem(long chatId, int playerLuck, Set<String> usedItemIds) {
        //TODO: make player luck affect rarity/expectedWeight
        List<ItemWeightProjection> itemWeights = itemRepository.getHighQualityItemWeights(chatId, usedItemIds);
        if (itemWeights.isEmpty()) {
            val optionalItem = itemRepository.findByChatIdAndMaxWeight(chatId, usedItemIds);
            if (optionalItem.isPresent()) {
                return switch (optionalItem.get().getItemType()) {
                    case WEAPON -> ItemMapper.INSTANCE.mapToWeapon(optionalItem.get());
                    case WEARABLE -> ItemMapper.INSTANCE.mapToWearable(optionalItem.get());
                    case USABLE -> ItemMapper.INSTANCE.mapToUsable(optionalItem.get());
                };
            } else {
                throw new EntityNotFoundException(chatId, "item", CallbackType.MENU_BACK);
            }
        }

        val weightLimit = getLimitWeightForSpecialItem(playerLuck, itemWeights.stream().map(ItemWeightProjection::getWeightAbs).collect(Collectors.toList()));
        val itemId = itemWeights.stream()
                .filter(itemWeight -> itemWeight.getWeightAbs() < weightLimit)
                .findFirst().map(ItemWeightProjection::getId)
                .orElseThrow(() -> new EntityNotFoundException(chatId, "item", CallbackType.MENU_BACK));
        return findItem(chatId, itemId);
    }

    private double getLimitWeightForSpecialItem(int playerLuck, List<Double> weights) {
        double max = weights.stream().mapToDouble(weight -> weight).max().getAsDouble();
        double min = weights.stream().mapToDouble(weight -> weight).min().getAsDouble();

        return (max - min) * playerLuck / 10.0;


    }

    /**
     * Looks for batch of items which summary weight
     * is as close as possible to expected
     *
     * @param chatId         current chat id
     * @param expectedWeight expected summary weight
     * @param maxItems       amount of items
     * @param usedItemIds    ids of items to exclude
     * @return found items
     */
    public Set<Item> getExpectedWeightItems(long chatId, Weight expectedWeight, Integer maxItems, Set<String> usedItemIds) {
        log.info("Collecting items...");
        log.info("Expected weight: {}, max items amount: {}", expectedWeight, maxItems);

        var weightsAbs = itemRepository.findClosestLesserWeight(chatId,
                        expectedWeight.toVector().getNorm(),
                        usedItemIds,
                        PageRequest.of(0, maxItems)).stream()
                .sorted(Comparator.comparing(ItemWeightProjection::getWeightAbs))
                .collect(Collectors.toCollection(ArrayList::new));

        var weightsAbsSum = weightsAbs.stream().mapToDouble(ItemWeightProjection::getWeightAbs).sum();

        while (abs(expectedWeight.toVector().getNorm() - weightsAbsSum) < expectedWeight.toVector().getNorm() - weightsAbsSum) {
            weightsAbs.removeLast();
            weightsAbs.add(itemRepository.findClosestLesserWeight(chatId, weightsAbs.getFirst().getWeightAbs(), usedItemIds,
                    PageRequest.of(0, 1)).getFirst());
            weightsAbsSum = weightsAbs.stream().mapToDouble(ItemWeightProjection::getWeightAbs).sum();
        }
        return findItems(chatId, weightsAbs.stream().map(ItemWeightProjection::getId).toList());
    }

    /**
     * Looks for item by given id in repository
     * throws {@link EntityNotFoundException} if none found
     *
     * @param chatId current chat id
     * @param itemId id of item to look for
     * @return found item
     */
    public Item findItem(Long chatId, String itemId) {
        val itemDocument = itemRepository.findByChatIdAndId(chatId, itemId).orElseThrow(() ->
                new EntityNotFoundException(chatId, "item", CallbackType.MENU_BACK, Pair.create("itemId", itemId))
        );
        switch (itemDocument.getItemType()) {
            case WEAPON -> {
                return ItemMapper.INSTANCE.mapToWeapon(itemDocument);
            }
            case WEARABLE -> {
                return ItemMapper.INSTANCE.mapToWearable(itemDocument);
            }
            case USABLE -> {
                return ItemMapper.INSTANCE.mapToUsable(itemDocument);
            }
        }
        return null;
    }

    /**
     * Removes all currents chat items from repository
     *
     * @param chatId id of current chat
     */
    public void dropCollection(Long chatId) {
        itemRepository.deleteAllByChatId(chatId);
    }

    /**
     * Saves list of items to repository
     *
     * @param items to save
     * @param <T>   class, extending {@link Item}
     * @return saved items
     */
    public <T extends Item> Set<Item> saveItems(List<T> items) {
        val itemDocuments = items.stream().map(ItemMapper.INSTANCE::mapToDocument).toList();
        val savedItemDocuments = itemRepository.saveAll(itemDocuments);
        return savedItemDocuments.stream().map(itemDocument -> switch (itemDocument.getItemType()) {
            case WEAPON -> ItemMapper.INSTANCE.mapToWeapon(itemDocument);
            case WEARABLE -> ItemMapper.INSTANCE.mapToWearable(itemDocument);
            case USABLE -> ItemMapper.INSTANCE.mapToUsable(itemDocument);
        }).collect(Collectors.toSet());
    }

    private Set<Item> findItems(Long chatId, List<String> itemIds) {
        val itemDocuments = itemRepository.findAllByChatIdAndIdIn(chatId, itemIds);
        var items = itemDocuments.stream()
                .map(itemDocument ->
                        switch (itemDocument.getItemType()) {
                            case WEAPON -> ItemMapper.INSTANCE.mapToWeapon(itemDocument);
                            case WEARABLE -> ItemMapper.INSTANCE.mapToWearable(itemDocument);
                            case USABLE -> ItemMapper.INSTANCE.mapToUsable(itemDocument);
                        })
                .peek(item -> {
                    if (isNull(item.getName())) {
                        itemNamingService.requestNameGeneration(item);
                    }
                })
                .collect(Collectors.toCollection(HashSet::new));
        while (items.stream().anyMatch(item -> isNull(item.getName()))) {
            items = items.stream().map(item -> {
                if (isNull(item.getName())) {
                    val updatedItemDocumentOptional = itemRepository.findByChatIdAndId(chatId, item.getId());
                    if (updatedItemDocumentOptional.isEmpty()) {
                        return item;
                    }
                    val updatedItemDocument = updatedItemDocumentOptional.get();
                    return switch (updatedItemDocument.getItemType()) {
                        case WEAPON -> ItemMapper.INSTANCE.mapToWeapon(updatedItemDocument);
                        case WEARABLE -> ItemMapper.INSTANCE.mapToWearable(updatedItemDocument);
                        case USABLE -> ItemMapper.INSTANCE.mapToUsable(updatedItemDocument);
                    };
                } else {
                    return item;
                }
            }).collect(Collectors.toCollection(HashSet::new));

        }
        return items;
    }
}
