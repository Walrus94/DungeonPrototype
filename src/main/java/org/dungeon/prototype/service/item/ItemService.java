package org.dungeon.prototype.service.item;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.document.item.ItemDocument;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.repository.ItemRepository;
import org.dungeon.prototype.repository.converters.mapstruct.ItemMapper;
import org.dungeon.prototype.repository.projections.ItemWeightProjection;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.math3.util.FastMath.max;
import static org.apache.commons.math3.util.FastMath.min;

@Slf4j
@Component
public class ItemService {
    private static final Double WEIGHT_PLAY_RANGE = 0.2;
    private final ItemMapper itemMapper = ItemMapper.INSTANCE;
    @Autowired
    private ItemGenerator itemGenerator;
    @Autowired
    private ItemNamingService itemNamingService;
    @Autowired
    private ItemRepository itemRepository;

    @Transactional
    public void generateItems(Long chatId) {
        CompletableFuture<Set<Weapon>> weaponsFuture = CompletableFuture.supplyAsync(() -> itemGenerator.generateWeapons(chatId));
        CompletableFuture<Set<Wearable>> wearablesFuture = CompletableFuture.supplyAsync(() -> itemGenerator.generateWearables(chatId));

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(weaponsFuture, wearablesFuture);

        try {
            combinedFuture.get();

            int count = itemRepository.saveAll(Stream.concat(weaponsFuture.get().stream(), wearablesFuture.get().stream())
                    .map(itemMapper::mapToDocument).collect(Collectors.toList())).size();
            log.debug("Generated {} items", count);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error on generating items: {}", e.getMessage());
        }
    }

    @Transactional
    public Wearable getMostLightweightWearable(Long chatId, WearableType wearableType) {
        List<ItemDocument> documents = itemRepository.findWearablesByChatIdTypeAndMinWeight(chatId, wearableType, PageRequest.of(0, 1));
        if (Objects.isNull(documents) || documents.isEmpty()) {
            log.error("Unable find most lightweight wearable of type {} for chat id {}!", wearableType, chatId);
            return null;
        }
        val document = documents.getFirst();
        if (Objects.isNull(document.getName())) {
            document.setName(itemNamingService.generateNames(Set.of(document.getAttributes())).get(document.getAttributes()));
            itemRepository.save(document);
        }
        return itemMapper.mapToWearable(document);
    }

    @Transactional
    public Weapon getMostLightWeightMainWeapon(Long chatId) {
        val documents = itemRepository.findMainWeaponByChatIdAndMinWeight(chatId, PageRequest.of(0, 1));
        if (Objects.isNull(documents) || documents.isEmpty()) {
            log.error("Unable to find most lightweight weapon for chat id {}!", chatId);
            return null;
        }

        val document = documents.getFirst();
        if (Objects.isNull(document.getName())) {
            document.setName(itemNamingService.generateNames(Set.of(document.getAttributes())).get(document.getAttributes()));
            itemRepository.save(document);
        }
        return itemMapper.mapToWeapon(document);
    }

    public Set<Item> getExpectedWeightItems(Long chatId, Integer expectedWeight, Integer maxItems, Set<String> usedItemIds) {
        log.debug("Collecting items...");
        Set<ItemDocument> items = new HashSet<>();
        log.debug("Expected weight: {}, max items amount: {}", expectedWeight, maxItems);

        val lowest = itemRepository.findFirstByOrderByWeightAsc(chatId, PageRequest.of(0, 1)).stream().map(ItemWeightProjection::getWeight).findFirst().get();
        val highest = itemRepository.findFirstByOrderByWeightDesc(chatId, PageRequest.of(0, 1)).stream().map(ItemWeightProjection::getWeight).findFirst().get();
        log.debug("Items weight range: {} - {}", lowest, highest);

        var weight = max(lowest, min(highest, expectedWeight / maxItems));
        while (items.stream().mapToInt(ItemDocument::getWeight).sum() - expectedWeight < expectedWeight * (1.0 + WEIGHT_PLAY_RANGE) && items.size() < maxItems) {
            weight = max(lowest, min(highest, (expectedWeight - items.stream().mapToInt(ItemDocument::getWeight).sum()) / (maxItems - items.size())));
            log.debug("Current weight: {}", weight);
            var foundItems = findItems(chatId, weight, maxItems - items.size(), usedItemIds);
            log.debug("Found items: {}", foundItems);
            if (Objects.nonNull(foundItems) && !foundItems.isEmpty()) {
                if (items.addAll(foundItems)) {
                    usedItemIds.addAll(foundItems.stream().map(ItemDocument::getId).toList());
                }
            }
        }
        return items.isEmpty() ? Collections.emptySet() : generateItemsNamesAndConvertFromDoc(items);
    }

    private List<ItemDocument> findItems(Long chatId, int weight, int limit, Set<String> usedItemIds) {
        log.debug("Searching for item weighted {}...", weight);

        val exactWeightItems = itemRepository.findByChatIdAndWeightAndIdNotIn(chatId, weight, usedItemIds, PageRequest.of(0, limit));
        if (Objects.nonNull(exactWeightItems) && !exactWeightItems.isEmpty()) {
            return exactWeightItems;
        }

        val closestLesserWeightList = itemRepository.findClosestLesserWeight(chatId, weight, usedItemIds, PageRequest.of(0, 1));
        val closestGreaterWeightList = itemRepository.findClosestGreaterWeight(chatId, weight, usedItemIds, PageRequest.of(0, 1));

        if (Objects.nonNull(closestLesserWeightList) && !closestLesserWeightList.isEmpty() &&
                Objects.nonNull(closestGreaterWeightList) && !closestGreaterWeightList.isEmpty()) {
            val closestLesserWeight = closestLesserWeightList.getFirst().getWeight();
            val closestGreaterWeight = closestGreaterWeightList.getFirst().getWeight();

            if (closestGreaterWeight - weight < weight - closestLesserWeight) {
                return itemRepository.findByChatIdAndWeightAndIdNotIn(chatId, closestGreaterWeight, usedItemIds, PageRequest.of(0, limit));
            } else {
                return itemRepository.findByChatIdAndWeightAndIdNotIn(chatId, closestLesserWeight, usedItemIds, PageRequest.of(0, limit));
            }
        } else {
            if (Objects.nonNull(closestLesserWeightList) && !closestLesserWeightList.isEmpty()) {
                val closestLesserWeight = closestLesserWeightList.getFirst().getWeight();
                return itemRepository.findByChatIdAndWeightAndIdNotIn(chatId, closestLesserWeight, usedItemIds, PageRequest.of(0, limit));
            }
            if (Objects.nonNull(closestGreaterWeightList) && !closestGreaterWeightList.isEmpty()) {
                val closestGreatestWeight = closestGreaterWeightList.getFirst().getWeight();
                return itemRepository.findByChatIdAndWeightAndIdNotIn(chatId, closestGreatestWeight, usedItemIds, PageRequest.of(0, limit));
            }
        }
        return Collections.emptyList();
    }

    @NotNull
    private Set<Item> generateItemsNamesAndConvertFromDoc(Set<ItemDocument> items) {
        val attributesNamesMap = itemNamingService.generateNames(items.stream().filter(item -> Objects.isNull(item.getName())).map(ItemDocument::getAttributes).collect(Collectors.toSet()));
        itemRepository.saveAll(items.stream().filter(itemDocument -> attributesNamesMap.containsKey(itemDocument.getAttributes()))
                .peek(itemDocument -> itemDocument.setName(attributesNamesMap.get(itemDocument.getAttributes())))
                .collect(Collectors.toSet()));
        return items.stream().map(itemDocument -> {
            if (itemDocument.getItemType().equals(ItemType.WEARABLE)) {
                return itemMapper.mapToWearable(itemDocument);
            } else if (itemDocument.getItemType().equals(ItemType.WEAPON)) {
                return itemMapper.mapToWeapon(itemDocument);
            } else return null;
        }).collect(Collectors.toSet());
    }

    public Item findItem(Long chatId, String itemId) {
        val itemDocument = itemRepository.findByChatIdAndId(chatId, itemId).orElseThrow(() -> {
            throw new NoSuchElementException();
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

    public void dropCollection(Long chatId) {
        itemRepository.deleteAllByChatId(chatId);
    }
}
