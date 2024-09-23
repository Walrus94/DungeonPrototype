package org.dungeon.prototype.service.item;

import jakarta.validation.constraints.NotNull;
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
import org.dungeon.prototype.repository.ItemRepository;
import org.dungeon.prototype.repository.converters.mapstruct.ItemMapper;
import org.dungeon.prototype.repository.projections.ItemWeightProjection;
import org.dungeon.prototype.service.item.generation.ItemNamingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
     * @param item to save
     * @return saved item
     */
    @Transactional
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
     * @param chatId current chat id
     * @param wearableType to look for
     * @return found item
     */
    @Transactional
    public Wearable getMostLightweightWearable(Long chatId, WearableType wearableType) {
        List<ItemDocument> documents = itemRepository.findWearablesByChatIdTypeAndMinWeight(chatId, wearableType, PageRequest.of(0, 1));
        if (Objects.isNull(documents) || documents.isEmpty()) {
            log.error("Unable find most lightweight wearable of type {} for chat id {}!", wearableType, chatId);
            throw new EntityNotFoundException(chatId, wearableType.toString(), CallbackType.DEFAULT_ERROR_RETURN);
        }
        val document = documents.getFirst();
        if (Objects.isNull(document.getName())) {
            document.setName(itemNamingService.generateNames(Set.of(document.getAttributes())).get(document.getAttributes()));
            itemRepository.save(document);
        }
        return itemMapper.mapToWearable(document);
    }

    /**
     * Looks for weapon of minimum weight
     * throws {@link EntityNotFoundException} if fails to find any
     * @param chatId current chat id
     * @return found item
     */
    @Transactional
    public Weapon getMostLightWeightMainWeapon(Long chatId) {
        val documents = itemRepository.findMainWeaponByChatIdAndMinWeight(chatId, PageRequest.of(0, 1));
        if (Objects.isNull(documents) || documents.isEmpty()) {
            log.error("Unable to find most lightweight weapon for chat id {}!", chatId);
            throw new EntityNotFoundException(chatId, "weapon", CallbackType.DEFAULT_ERROR_RETURN);
        }

        val document = documents.getFirst();
        if (Objects.isNull(document.getName())) {
            document.setName(itemNamingService.generateNames(Set.of(document.getAttributes())).get(document.getAttributes()));
            itemRepository.save(document);
        }
        return itemMapper.mapToWeapon(document);
    }

    /**
     * Looks for batch of items which summary weight
     * is as close as possible to expected
     * @param chatId current chat id
     * @param expectedWeight expected summary weight
     * @param maxItems amount of items
     * @param usedItemIds ids of items to exclude
     * @return found items
     */
    public Set<Item> getExpectedWeightItems(Long chatId, Weight expectedWeight, Integer maxItems, Set<String> usedItemIds) {
        log.debug("Collecting items...");
        log.debug("Expected weight: {}, max items amount: {}", expectedWeight, maxItems);

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
        val items = findItems(chatId, weightsAbs.stream().map(ItemWeightProjection::getId).toList());
        return items.isEmpty() ? Collections.emptySet() : generateItemsNamesAndUpdate(items);
    }

    /**
     * Looks for item by given id in repository
     * throws {@link EntityNotFoundException} if none found
     * @param chatId current chat id
     * @param itemId id of item to look for
     * @return found item
     */
    public Item findItem(Long chatId, String itemId) {
        val itemDocument = itemRepository.findByChatIdAndId(chatId, itemId).orElseThrow(() -> {
            throw new EntityNotFoundException(chatId, "item", CallbackType.DEFAULT_ERROR_RETURN, Pair.create("itemId", itemId));
        });
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
     * @param chatId id of current chat
     */
    public void dropCollection(Long chatId) {
        itemRepository.deleteAllByChatId(chatId);
    }

    /**
     * Saves list of items to repository
     * @param items to save
     * @param <T> class, extending {@link Item}
     * @return saved items
     */
    @Transactional
    public <T extends Item> List<Item> saveItems(List<T> items) {
        val itemDocuments = items.stream().map(ItemMapper.INSTANCE::mapToDocument).toList();
        val savedItemDocuments = itemRepository.saveAll(itemDocuments);
        return savedItemDocuments.stream().map(itemDocument -> switch (itemDocument.getItemType()) {
            case WEAPON -> ItemMapper.INSTANCE.mapToWeapon(itemDocument);
            case WEARABLE -> ItemMapper.INSTANCE.mapToWearable(itemDocument);
            case USABLE -> ItemMapper.INSTANCE.mapToUsable(itemDocument);
        }).toList();
    }
    private Set<Item> findItems(Long chatId, List<String> itemIds) {
        val itemDocuments = itemRepository.findAllByChatIdAndIdIn(chatId, itemIds);
        return itemDocuments.stream().map(itemDocument ->
                        switch (itemDocument.getItemType()) {
                            case WEAPON -> ItemMapper.INSTANCE.mapToWeapon(itemDocument);
                            case WEARABLE -> ItemMapper.INSTANCE.mapToWearable(itemDocument);
                            case USABLE -> ItemMapper.INSTANCE.mapToUsable(itemDocument);
                        })
                .collect(Collectors.toCollection(HashSet::new));
    }

    //TODO: parallel item naming
    @NotNull
    private Set<Item> generateItemsNamesAndUpdate(Set<Item> items) {
        val attributesNamesMap = itemNamingService.generateNames(items.stream().filter(item -> Objects.isNull(item.getName())).map(Item::getAttributes).collect(Collectors.toSet()));
        return items.stream().filter(item -> attributesNamesMap.containsKey(item.getAttributes()))
                .peek(item -> {
                    item.setName(attributesNamesMap.get(item.getAttributes()));
                    saveItem(item);
                })
                .collect(Collectors.toSet());
    }
}
