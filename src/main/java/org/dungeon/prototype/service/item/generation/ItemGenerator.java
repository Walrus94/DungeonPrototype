package org.dungeon.prototype.service.item.generation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.async.AsyncJobHandler;
import org.dungeon.prototype.async.TaskType;
import org.dungeon.prototype.exception.EntityNotFoundException;
import org.dungeon.prototype.model.effect.AdditionEffect;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.MultiplicationEffect;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.dungeon.prototype.model.inventory.attributes.Quality;
import org.dungeon.prototype.model.inventory.attributes.weapon.Handling;
import org.dungeon.prototype.model.inventory.attributes.weapon.Size;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponHandlerMaterial;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponMaterial;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponType;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.properties.GenerationProperties;
import org.dungeon.prototype.service.effect.ItemEffectsGenerator;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Handling.SINGLE_HANDED;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Handling.TWO_HANDED;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Size.LARGE;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Size.MEDIUM;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Size.SMALL;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.BLUNT;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.SLASH;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.STAB;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.STRIKE;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponHandlerMaterial.LEATHER;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponHandlerMaterial.TREATED_LEATHER;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponMaterial.DIAMOND;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponMaterial.DRAGON_BONE;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponMaterial.ENCHANTED_WOOD;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponMaterial.OBSIDIAN;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponMaterial.PLATINUM;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponMaterial.STONE;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponMaterial.WOOD;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponType.DAGGER;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponType.STAFF;
import static org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial.CLOTH;
import static org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial.ELVEN_SILK;
import static org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial.IRON;
import static org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial.MITHRIL;
import static org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial.STEEL;
import static org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial.WOOL;
import static org.dungeon.prototype.util.GenerationUtil.applyAdjustment;
import static org.dungeon.prototype.util.GenerationUtil.multiplyAllParametersBy;
import static org.dungeon.prototype.util.RandomUtil.getRandomEnumValue;
import static org.dungeon.prototype.util.RandomUtil.getRandomMagicType;
import static org.dungeon.prototype.util.RandomUtil.getRandomWeightedEnumValue;

@Slf4j
@Service
public class ItemGenerator {
    @Value("${generation.items.weapon.weapon-attributes-pool-size}")
    private Integer weaponAttributesPoolSize;
    @Value("${generation.items.weapon.weapon-per-game}")
    private Integer weaponPerGame;
    @Value("${generation.items.wearables.wearable-attribute-pool-size}")
    private Integer wearableAttributesPoolSize;
    @Value("${generation.items.wearables.wearables-per-game}")
    private Integer wearablesPerGame;
    @Value("${generation.items.wearables.default-armor}")
    private Integer defaultArmor;
    @Autowired
    private ItemService itemService;
    @Autowired
    private AsyncJobHandler asyncJobHandler;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ItemEffectsGenerator itemEffectsGenerator;
    @Autowired
    private GenerationProperties generationProperties;

    /**
     * Generates items for game: runs two async generators
     * for {@link Wearable} and {@link Weapon} items
     * for game and stores results to repository.
     * Items should be removed when game is over
     *
     * @param chatId id of chat where game runs
     */
    public void generateItems(Long chatId) {
        messageService.sendItemsGeneratingInfoMessage(chatId);

        asyncJobHandler.submitItemGenerationTask(() -> generateWeapons(chatId), TaskType.WEAPON_GENERATION, chatId);
        asyncJobHandler.submitItemGenerationTask(() -> generateWearables(chatId), TaskType.WEARABLE_GENERATION, chatId);

        asyncJobHandler.submitEffectGenerationTask(() -> addEffects(chatId, weaponPerGame, wearablesPerGame), TaskType.EFFECTS_GENERATION, chatId);
    }

    private void generateWeapons(Long chatId) {
        log.info("Generation weapons for chatId:{}...", chatId);
        Set<WeaponAttributes> generatedAttributesCombinations = new HashSet<>();
        while (generatedAttributesCombinations.size() < weaponAttributesPoolSize) {
            val type = getRandomEnumValue(List.of(WeaponType.values()));
            Handling handling;
            switch (type) {
                case SWORD, AXE -> handling = getRandomEnumValue(List.of(SINGLE_HANDED, TWO_HANDED));
                case DAGGER -> handling = SINGLE_HANDED;
                default -> handling = getRandomEnumValue(List.of(Handling.values())); //TODO: consider more constraints
            }
            WeaponMaterial weaponMaterial;
            switch (type) {
                case SWORD, AXE, DAGGER ->
                        weaponMaterial = getRandomEnumValue(List.of(WeaponMaterial.IRON, WeaponMaterial.STEEL, PLATINUM, DIAMOND, WeaponMaterial.MITHRIL, DRAGON_BONE));
                case CLUB -> weaponMaterial = getRandomEnumValue(List.of(WOOD, STONE, ENCHANTED_WOOD));
                case MACE ->
                        weaponMaterial = getRandomEnumValue(List.of(WeaponMaterial.STONE, WeaponMaterial.IRON, WeaponMaterial.STEEL, PLATINUM, DIAMOND, WeaponMaterial.MITHRIL, OBSIDIAN, DRAGON_BONE, ENCHANTED_WOOD));
                case STAFF ->
                        weaponMaterial = getRandomEnumValue(List.of(WOOD, STONE, OBSIDIAN, WeaponMaterial.MITHRIL, DRAGON_BONE, ENCHANTED_WOOD));
                default -> weaponMaterial = getRandomEnumValue(List.of(WeaponMaterial.values()));
            }
            WeaponHandlerMaterial weaponHandlerMaterial;
            switch (weaponMaterial) {
                case WOOD ->
                        weaponHandlerMaterial = getRandomEnumValue(List.of(WeaponHandlerMaterial.WOOD, WeaponHandlerMaterial.LEATHER, TREATED_LEATHER));
                case STONE ->
                        weaponHandlerMaterial = getRandomEnumValue(List.of(WeaponHandlerMaterial.LEATHER, TREATED_LEATHER));
                case ENCHANTED_WOOD ->
                        weaponHandlerMaterial = getRandomEnumValue(List.of(WeaponHandlerMaterial.WOOD, LEATHER, TREATED_LEATHER));
                default -> weaponHandlerMaterial = getRandomEnumValue(List.of(WeaponHandlerMaterial.values()));
            }
            Quality quality;
            if (DRAGON_BONE.equals(weaponMaterial) && WeaponHandlerMaterial.DRAGON_BONE.equals(weaponHandlerMaterial)) {
                quality = Quality.MYTHIC;
            } else {
                quality = getRandomWeightedEnumValue(List.of(Quality.values()));
            }
            Size size;
            if (TWO_HANDED.equals(handling)) {
                size = LARGE;
            } else {
                if (DAGGER.equals(type)) {
                    size = getRandomEnumValue(List.of(SMALL, MEDIUM));
                } else {
                    size = getRandomEnumValue(List.of(Size.values()));
                }
            }
            WeaponAttackType weaponAttackType;
            switch (type) {
                case SWORD -> weaponAttackType = getRandomEnumValue(List.of(STAB, SLASH));
                case AXE -> weaponAttackType = SLASH;
                case DAGGER, SPEAR -> weaponAttackType = STAB;
                case CLUB -> weaponAttackType = BLUNT;
                case MACE -> weaponAttackType = getRandomEnumValue(List.of(SLASH, BLUNT));
                default -> weaponAttackType = STRIKE;
            }
            val attributes = new WeaponAttributes();
            attributes.setWeaponType(type);
            attributes.setHandling(handling);
            attributes.setWeaponMaterial(weaponMaterial);
            attributes.setWeaponHandlerMaterial(weaponHandlerMaterial);
            attributes.setQuality(quality);
            attributes.setSize(size);
            attributes.setWeaponAttackType(weaponAttackType);
            log.debug("Random attributes combination: {}", attributes);
            if (generatedAttributesCombinations.contains(attributes)) {
                log.debug("Already processed, skipping to next combination...");
                continue;
            }
            generatedAttributesCombinations.add(attributes);
        }

        val vanillaWeapons = generatedAttributesCombinations.stream()
                .map(attributes -> generateVanillaWeapon(attributes, chatId))
                .collect(Collectors.toList());
        val savedItems = itemService.saveItems(vanillaWeapons);
        log.info("{} weapons without effects generated.", savedItems.size());
    }


    private void generateWearables(Long chatId) {
        log.info("Generation wearables for chatId:{}...", chatId);
        Set<WearableAttributes> attributesCombinations = new HashSet<>();
        Map<WearableType, Integer> typesCount = new HashMap<>();
        while (attributesCombinations.size() < wearableAttributesPoolSize) {
            var type = getRandomEnumValue(List.of(WearableType.values()));
            WearableMaterial material = null;
            switch (type) {
                case VEST -> {
                    material = RandomUtil.getRandomEnumValue(List.of(WearableMaterial.values()));
                    if (typesCount.containsKey(WearableType.VEST)) {
                        typesCount.put(WearableType.VEST, typesCount.get(WearableType.VEST) + 1);
                    } else {
                        typesCount.put(WearableType.VEST, 1);
                    }
                }
                case HELMET -> {
                    material = getRandomEnumValue(List.of(IRON, STEEL, MITHRIL));
                    if (typesCount.containsKey(WearableType.HELMET)) {
                        typesCount.put(WearableType.HELMET, typesCount.get(WearableType.HELMET) + 1);
                    } else {
                        typesCount.put(WearableType.HELMET, 1);
                    }
                }
                case GLOVES -> {
                    material = getRandomEnumValue(List.of(WearableMaterial.values()));
                    if (typesCount.containsKey(WearableType.GLOVES)) {
                        typesCount.put(WearableType.GLOVES, typesCount.get(WearableType.GLOVES) + 1);
                    } else {
                        typesCount.put(WearableType.GLOVES, 1);
                    }
                }
                case BOOTS -> {
                    material = RandomUtil.getRandomEnumValue(List.of(WearableMaterial.values()), List.of(CLOTH, ELVEN_SILK, WOOL));
                    if (typesCount.containsKey(WearableType.BOOTS)) {
                        typesCount.put(WearableType.BOOTS, typesCount.get(WearableType.BOOTS) + 1);
                    } else {
                        typesCount.put(WearableType.BOOTS, 1);
                    }
                }
            }
            val quality = getRandomEnumValue(List.of(Quality.values()));
            val wearableAttributes = new WearableAttributes();
            wearableAttributes.setWearableType(type);
            wearableAttributes.setQuality(quality);
            wearableAttributes.setWearableMaterial(material);
            log.debug("Random attributes combination: {}", wearableAttributes);
            if (attributesCombinations.contains(wearableAttributes)) {
                log.debug("Already processed, skipping to next combination...");
                continue;
            }
            attributesCombinations.add(wearableAttributes);
        }
        val vanillaWearables = attributesCombinations.stream()
                .map(attributes -> generateVanillaWearable(attributes, chatId))
                .collect(Collectors.toList());
        val savedItems = itemService.saveItems(vanillaWearables);
        log.info("{} wearables without effects generated.", savedItems.size());
    }

    private void addEffects(Long chatId, int weaponLimit, int wearableLimit) {
        val weightScale = itemService.findAllItemsWeightsByChatId(chatId).entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        TreeMap::new,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toCollection(LinkedList::new))
                ));
        log.info("Adding effects to generated items, amount: {}", weightScale.size());
        double startSegmentWeight = 0.0;
        int weaponCount = 0;
        int wearableCount = 0;
        val vanillaItemsIds = weightScale.values().stream().flatMap(Collection::stream).toList();
        while (weaponCount < weaponLimit || wearableCount < wearableLimit) {
            val multipleItemsWeight = weightScale.entrySet().stream().filter(e -> e.getValue().size() > 0).toList();
            double largestSegment = 0.0;
            for (Map.Entry<Double, LinkedList<String>> entry : multipleItemsWeight.size() > 0 ? multipleItemsWeight : weightScale.entrySet()) {
                if (entry.getKey() - weightScale.lowerKey(entry.getKey()) > weightScale.higherKey(entry.getKey()) - entry.getKey()) {
                    double segment = entry.getKey() - weightScale.lowerKey(entry.getKey());
                    if (segment > largestSegment) {
                        largestSegment = segment;
                        startSegmentWeight = weightScale.lowerKey(entry.getKey());
                    }
                } else {
                    double segment = weightScale.higherKey(entry.getKey()) - entry.getKey();
                    if (segment > largestSegment) {
                        largestSegment = segment;
                        startSegmentWeight = entry.getKey();
                    }
                }
            }
            double initialWeight;
            double expectedWeightChange;
            if (weightScale.get(startSegmentWeight).size() >= weightScale.higherEntry(startSegmentWeight).getValue().size() ||
                    getNegativeEffectCandidate(chatId, weightScale.get(weightScale.higherKey(startSegmentWeight))).isEmpty()
            ) {
                initialWeight = startSegmentWeight;
                expectedWeightChange = (initialWeight * weightScale.get(startSegmentWeight).size()) / (largestSegment * largestSegment);
            } else {
                initialWeight = weightScale.higherKey(startSegmentWeight);
                expectedWeightChange =  - (initialWeight * weightScale.get(initialWeight).size()) / (largestSegment * largestSegment);
            }
            String itemId;
            if (expectedWeightChange < 0.0) {
                itemId = getNegativeEffectCandidate(chatId, weightScale.get(initialWeight)).orElseThrow(() -> new EntityNotFoundException(chatId, "item", CallbackType.MENU_BACK));
            } else {
                itemId = weightScale.get(initialWeight).get(0);
            }
            if (vanillaItemsIds.contains(itemId)) {
                val newItemData = itemEffectsGenerator.copyItemAndAddEffect(chatId, itemId, expectedWeightChange);
                if (nonNull(newItemData)) {
                    insertNewItem(newItemData, weightScale);
                }
            } else {
                val updatedWeight = itemEffectsGenerator.addItemEffect(chatId, itemId, expectedWeightChange);
                if (updatedWeight.isPresent()) {
                    val oldValueList = weightScale.get(initialWeight);
                    if (oldValueList.size() == 1) {
                        weightScale.remove(initialWeight);
                    } else {
                        oldValueList.remove(itemId);
                    }
                    insertNewItem(Pair.create(itemId, updatedWeight.get()), weightScale);
                }
            }

            switch (itemService.findItem(chatId, itemId).getItemType()) {
                case WEAPON:
                    weaponCount++;
                case WEARABLE:
                    wearableCount++;
            }
        }
    }

    private Optional<String> getNegativeEffectCandidate(long chatId, List<String> itemIds) {
        return itemIds.stream()
                .filter(itemId -> {
                    val item = itemService.findItem(chatId, itemId);
                    if (nonNull(item)) {
                        return item.getEffects().stream()
                                .map(effect -> isNegative(effect) ? -effect.getWeight().toVector().getNorm() : effect.getWeight().toVector().getNorm())
                                .reduce(0.0, Double::sum) > 0.0;
                    }
                    return false;
                })
                .findFirst();
    }

    private boolean isNegative(Effect effect) {
        if (effect instanceof AdditionEffect additionEffect) {
            return additionEffect.getAmount() < 0;
        } else if (effect instanceof MultiplicationEffect multiplicationEffect) {
            return multiplicationEffect.getMultiplier() < 1.0;
        }

        return false;
    }


    private void insertNewItem(Pair<String, Double> newItemData, Map<Double, LinkedList<String>> weightScale) {
        val newItemId = newItemData.getFirst();
        val newItemWeight = newItemData.getSecond();
        weightScale.computeIfPresent(newItemWeight, (k, v) -> {
            v.add(newItemId);
            return v;
        });
        weightScale.putIfAbsent(newItemWeight, new LinkedList<>(List.of(newItemId)));
    }

    private Weapon generateVanillaWeapon(WeaponAttributes weaponAttributes, Long chatId) {
        log.info("Generating weapon...");
        val weapon = new Weapon();
        weapon.setAttributes(weaponAttributes);
        weapon.setChatId(chatId);
        weapon.setEffects(Collections.emptyList());
        return calculateParameters(weapon);
    }

    private Weapon calculateParameters(Weapon weapon) {
        log.info("Calculating weapon params...");
        val properties = generationProperties.getItems().getWeapon();
        log.debug("Loaded properties: {}", properties);
        val defaultAttributesMap = properties.getDefaultAttributes();
        val defaultAttributes = defaultAttributesMap.get(weapon.getAttributes().getWeaponType());
        log.debug("Default attributes: {}", defaultAttributes);
        weapon.setAttack(defaultAttributes.getAttack());
        weapon.setCriticalHitChance(defaultAttributes.getCriticalHitChance());
        weapon.setCriticalHitMultiplier(defaultAttributes.getCriticalHitMultiplier());
        weapon.setChanceToMiss(defaultAttributes.getChanceToMiss());
        weapon.setChanceToKnockOut(defaultAttributes.getChanceToKnockOut());
        if (STAFF.equals(weapon.getAttributes().getWeaponType())) {
            weapon.setMagicType(getRandomMagicType());
        }

        val handlingAdjustmentAttributes = properties.getHandlingAdjustmentAttributes().get(weapon.getAttributes().getHandling());
        log.debug("Handling adjustment attributes: {}", handlingAdjustmentAttributes);
        applyAdjustment(weapon, handlingAdjustmentAttributes);

        val weaponMaterialAdjustmentAttributes = properties.getWeaponMaterialAdjustmentAttributes().get(weapon.getAttributes().getWeaponMaterial());
        log.debug("Weapon material adjustment attributes: {}", weaponMaterialAdjustmentAttributes);
        applyAdjustment(weapon, weaponMaterialAdjustmentAttributes);
        if (ENCHANTED_WOOD.equals(weapon.getAttributes().getWeaponMaterial()) && isNull(weapon.getMagicType())) {
            weapon.setMagicType(getRandomMagicType());
        }
        if (DRAGON_BONE.equals(weapon.getAttributes().getWeaponMaterial()) && WeaponHandlerMaterial.DRAGON_BONE.equals(weapon.getAttributes().getWeaponHandlerMaterial())) {
            log.info("Complete Dragon bone!");
            weapon.setIsCompleteDragonBone(true);
            weapon.setMagicType(MagicType.of(0.0, 1.0));
        } else {
            weapon.setIsCompleteDragonBone(false);
        }

        val completeMaterialAdjustmentAttributes = properties.getCompleteMaterialAdjustmentAttributes();
        val handlerMaterial = getEqualWeaponHandlerMaterial(weapon);
        if (handlerMaterial.isPresent() && completeMaterialAdjustmentAttributes.containsKey(handlerMaterial.get())) {
            log.info("Weapon and handler materials matched, {}", handlerMaterial.get());
            val completeMaterialAdjustment = completeMaterialAdjustmentAttributes.get(handlerMaterial.get());
            log.debug("Complete material adjustment attributes: {}", completeMaterialAdjustment);
            applyAdjustment(weapon, completeMaterialAdjustment);
        } else {
            log.info("Weapon and handler materials differs!");
            val weaponHandlerAdjustment = properties.getWeaponHandlerMaterialAdjustmentAttributes().get(weapon.getAttributes().getWeaponHandlerMaterial());
            log.debug("Weapon handler adjustment attributes: {}", weaponHandlerAdjustment);
            applyAdjustment(weapon, weaponHandlerAdjustment);
        }
        val sizeAdjustment = properties.getSizeAdjustmentAttributes().get(weapon.getAttributes().getSize());
        log.debug("Size adjustment attributes: {}", sizeAdjustment);
        weapon.setAttack((int) (weapon.getAttack() * sizeAdjustment.getAttackRatio()));
        weapon.setChanceToMiss(weapon.getChanceToMiss() * sizeAdjustment.getChanceToMissRatio());

        val attackTypeAdjustment = properties.getAttackTypeAdjustmentAttributes().get(weapon.getAttributes().getWeaponAttackType());
        log.debug("Attack type adjustment attributes: {}", attackTypeAdjustment);
        applyAdjustment(weapon, attackTypeAdjustment);
        val qualityAdjustmentRatio = properties.getQualityAdjustmentRatio().get(weapon.getAttributes().getQuality());
        log.debug("Quality adjustment ratio: {}", qualityAdjustmentRatio);
        multiplyAllParametersBy(weapon, qualityAdjustmentRatio);
        if (isNull(weapon.getMagicType())) {
            weapon.setMagicType(MagicType.of(0.0, 0.0));
        }
        return weapon;
    }

    private static Optional<WeaponHandlerMaterial> getEqualWeaponHandlerMaterial(Weapon weapon) {
        try {
            val weaponHandlerMaterial = WeaponHandlerMaterial.valueOf(weapon.getAttributes().getWeaponMaterial().toString());
            return Optional.of(weaponHandlerMaterial);
        } catch (IllegalArgumentException e) {
            log.debug("Handler material doesn't match! Exception thrown: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Wearable generateVanillaWearable(WearableAttributes wearableAttributes, Long chatId) {
        val wearable = new Wearable();
        wearable.setAttributes(wearableAttributes);
        wearable.setChatId(chatId);
        wearable.setEffects(Collections.emptyList());
        return calculateParameters(wearable);
    }

    private Wearable calculateParameters(Wearable wearable) {
        if (wearable.getAttributes().getWearableMaterial().equals(WearableMaterial.ENCHANTED_LEATHER)) {
            wearable.setMagicType(getRandomMagicType());
        } else {
            wearable.setMagicType(MagicType.of(0.0, 0.0));
        }
        var armor = defaultArmor;
        var chanceToDodge = 0.0;
        val wearablesProperties = generationProperties.getItems().getWearables();
        if (WearableType.BOOTS.equals(wearable.getAttributes().getWearableType())) {
            chanceToDodge = wearablesProperties.getChanceToDodgeRatio().get(wearable.getAttributes().getWearableMaterial());
        }
        armor += wearablesProperties.getArmorBonus().get(wearable.getAttributes().getWearableMaterial());
        val qualityRatio = wearablesProperties.getQualityAdjustmentRatio().get(wearable.getAttributes().getQuality());
        armor = (int) (armor * qualityRatio);
        chanceToDodge *= qualityRatio;
        wearable.setArmor(armor);
        wearable.setChanceToDodge(chanceToDodge);
        return wearable;
    }
}
