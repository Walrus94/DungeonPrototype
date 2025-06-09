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
import org.dungeon.prototype.service.balancing.BalanceMatrixService;
import org.dungeon.prototype.service.effect.ItemEffectsGenerator;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.stats.GameResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Handling.MAIN;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Handling.SECONDARY;
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
import static org.dungeon.prototype.util.RandomUtil.getRandomMagicType;
import static org.dungeon.prototype.util.RandomUtil.getRandomWeightedEnumValue;

@Slf4j
@Service
public class ItemGenerator {
    @Value("${generation.items.weapon.weapon-attributes-pool-size}")
    private Integer weaponAttributesPoolSize;
    @Value("${generation.items.weapon.weapon-attribute-vector-size}")
    private Integer weaponAttributeVectorSize;
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
    private BalanceMatrixService balanceMatrixService;
    @Autowired
    private GameResultService gameResultService;
    @Autowired
    private ItemEffectsGenerator itemEffectsGenerator;

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

        asyncJobHandler.submitEffectGenerationTask(() -> addEffects(chatId), TaskType.EFFECTS_GENERATION, chatId);
    }

    private void generateWeapons(Long chatId) {
        log.info("Generation weapons for chatId:{}...", chatId);
        val defaultAttributesMatrix = balanceMatrixService.getBalanceMatrix(chatId, "weapon_type_attr");
        val weaponHandlingAdjustmentMatrix = balanceMatrixService.getBalanceMatrix(chatId, "weapon_handling_type_adjustment");
        val weaponMaterialAdjustmentMatrix = balanceMatrixService.getBalanceMatrix(chatId, "weapon_material_adjustment");
        val weaponHandlerMaterialAdjustmentMatrix = balanceMatrixService.getBalanceMatrix(chatId, "weapon_handler_material_adjustment");
        val qualityAdjustmentMatrix = balanceMatrixService.getBalanceMatrix(chatId, "items_quality_adjustment");
        val sizeAdjustmentMatrix = balanceMatrixService.getBalanceMatrix(chatId, "weapon_size_adjustment");
        val weaponAttackTypeAdjustmentMatrix = balanceMatrixService.getBalanceMatrix(chatId, "weapon_attack_type_adjustment");
        Set<WeaponAttributes> generatedAttributesCombinations = new HashSet<>();
        while (generatedAttributesCombinations.size() < weaponAttributesPoolSize) {
            val type = getRandomWeightedEnumValue(generateAttributeMap(defaultAttributesMatrix, WeaponType.class));
            Handling handling;
            switch (type) {
                case SWORD, AXE ->
                        handling = getRandomWeightedEnumValue(generateAttributeMap(weaponHandlingAdjustmentMatrix, SINGLE_HANDED, TWO_HANDED, MAIN));
                case DAGGER ->
                        handling = getRandomWeightedEnumValue(generateAttributeMap(weaponHandlingAdjustmentMatrix, SINGLE_HANDED, SECONDARY));
                default ->
                        handling = getRandomWeightedEnumValue(generateAttributeMap(weaponHandlingAdjustmentMatrix, Handling.class));
            }
            WeaponMaterial weaponMaterial;
            switch (type) {
                case SWORD, AXE, DAGGER -> weaponMaterial =
                        getRandomWeightedEnumValue(generateAttributeMap(weaponMaterialAdjustmentMatrix,
                                WeaponMaterial.IRON,
                                WeaponMaterial.STEEL,
                                PLATINUM,
                                DIAMOND,
                                WeaponMaterial.MITHRIL,
                                DRAGON_BONE));
                case CLUB -> weaponMaterial =
                        getRandomWeightedEnumValue(generateAttributeMap(weaponMaterialAdjustmentMatrix, WOOD, STONE, ENCHANTED_WOOD));
                case MACE -> weaponMaterial =
                        getRandomWeightedEnumValue(generateAttributeMap(weaponMaterialAdjustmentMatrix,
                                WeaponMaterial.STONE,
                                WeaponMaterial.IRON,
                                WeaponMaterial.STEEL,
                                PLATINUM,
                                DIAMOND,
                                WeaponMaterial.MITHRIL,
                                OBSIDIAN,
                                DRAGON_BONE,
                                ENCHANTED_WOOD));
                case STAFF -> weaponMaterial =
                        getRandomWeightedEnumValue(generateAttributeMap(weaponMaterialAdjustmentMatrix,
                                WOOD,
                                STONE,
                                OBSIDIAN,
                                WeaponMaterial.MITHRIL,
                                DRAGON_BONE,
                                ENCHANTED_WOOD));
                default ->
                        weaponMaterial = getRandomWeightedEnumValue(generateAttributeMap(weaponMaterialAdjustmentMatrix, WeaponMaterial.values()));
            }
            WeaponHandlerMaterial weaponHandlerMaterial;
            switch (weaponMaterial) {
                case WOOD -> weaponHandlerMaterial =
                        getRandomWeightedEnumValue(generateAttributeMap(weaponHandlerMaterialAdjustmentMatrix,
                                WeaponHandlerMaterial.WOOD,
                                WeaponHandlerMaterial.LEATHER,
                                TREATED_LEATHER));
                case STONE -> weaponHandlerMaterial =
                        getRandomWeightedEnumValue(generateAttributeMap(weaponHandlerMaterialAdjustmentMatrix,
                                WeaponHandlerMaterial.LEATHER, TREATED_LEATHER));
                case ENCHANTED_WOOD -> weaponHandlerMaterial =
                        getRandomWeightedEnumValue(generateAttributeMap(weaponHandlerMaterialAdjustmentMatrix,
                                WeaponHandlerMaterial.WOOD, LEATHER, TREATED_LEATHER));
                default -> weaponHandlerMaterial =
                        getRandomWeightedEnumValue(generateAttributeMap(weaponHandlerMaterialAdjustmentMatrix, WeaponHandlerMaterial.class));
            }
            Quality quality;
            if (DRAGON_BONE.equals(weaponMaterial) && WeaponHandlerMaterial.DRAGON_BONE.equals(weaponHandlerMaterial)) {
                quality = Quality.MYTHIC;
            } else {
                quality = getRandomWeightedEnumValue(generateAttributeMap(qualityAdjustmentMatrix, Quality.class));
            }
            Size size;
            if (TWO_HANDED.equals(handling)) {
                size = LARGE;
            } else {
                if (DAGGER.equals(type)) {
                    size = getRandomWeightedEnumValue(generateAttributeMap(sizeAdjustmentMatrix, SMALL, MEDIUM));
                } else {
                    size = getRandomWeightedEnumValue(generateAttributeMap(sizeAdjustmentMatrix, Size.class));
                }
            }
            WeaponAttackType weaponAttackType;
            switch (type) {
                case SWORD -> weaponAttackType =
                        getRandomWeightedEnumValue(generateAttributeMap(weaponAttackTypeAdjustmentMatrix, STAB, SLASH));
                case AXE -> weaponAttackType = SLASH;
                case DAGGER, SPEAR -> weaponAttackType = STAB;
                case CLUB -> weaponAttackType = BLUNT;
                case MACE -> weaponAttackType =
                        getRandomWeightedEnumValue(generateAttributeMap(weaponAttackTypeAdjustmentMatrix, SLASH, BLUNT));
                case STAFF -> weaponAttackType = STRIKE;
                default -> weaponAttackType =
                        getRandomWeightedEnumValue(generateAttributeMap(weaponAttackTypeAdjustmentMatrix, WeaponAttackType.class));
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
        Map<WearableMaterial, Double> wearableMaterialAdjustmentVector = getWearableMaterialAdjustmentVector(chatId);
        val qualityAdjustmentMatrix = balanceMatrixService.getBalanceMatrix(chatId, "items_quality_adjustment");
        while (attributesCombinations.size() < wearableAttributesPoolSize) {
            var type = getRandomWeightedEnumValue(normalizeMap(
                    Arrays.stream(WearableType.values())
                            .collect(Collectors.toMap(Function.identity(),
                                    v -> 1.0 / (typesCount.getOrDefault(v, 1))))));
            WearableMaterial material = null;
            switch (type) {
                case VEST, GLOVES -> material =
                        getRandomWeightedEnumValue(wearableMaterialAdjustmentVector);
                case HELMET -> material =
                        getRandomWeightedEnumValue(wearableMaterialAdjustmentVector.entrySet()
                                .stream()
                                .filter(entry -> !List.of(IRON, STEEL, MITHRIL).contains(entry.getKey()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                case BOOTS -> material =
                        getRandomWeightedEnumValue(wearableMaterialAdjustmentVector.entrySet()
                                .stream()
                                .filter(entry -> !List.of(CLOTH, ELVEN_SILK, WOOL).contains(entry.getKey()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            }
            val quality = getRandomWeightedEnumValue(generateAttributeMap(qualityAdjustmentMatrix, Quality.class));
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
            if (typesCount.containsKey(type)) {
                typesCount.put(type, typesCount.get(type) + 1);
            } else {
                typesCount.put(type, 1);
            }
        }
        val vanillaWearables = attributesCombinations.stream()
                .map(attributes -> generateVanillaWearable(attributes, chatId))
                .collect(Collectors.toList());
        val savedItems = itemService.saveItems(vanillaWearables);
        log.info("{} wearables without effects generated.", savedItems.size());
    }

    private Map<WearableMaterial, Double> getWearableMaterialAdjustmentVector(long chatId) {
        val armorBonus = balanceMatrixService.getBalanceMatrixColumn(chatId, "wearable_armor_bonus", 0);
        val chanceToDodgeAdjustment = balanceMatrixService.getBalanceMatrixColumn(chatId, "wearable_chance_to_dodge_adjustment", 0);

        return normalizeMap(Arrays.stream(WearableMaterial.values()).collect(Collectors.toMap(Function.identity(), v -> 1 / (defaultArmor + armorBonus[v.ordinal()] + chanceToDodgeAdjustment[v.ordinal()]))));
    }

    private void addEffects(Long chatId) {
        val weightScale = itemService.findAllItemsWeightsByChatId(chatId).entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        TreeMap::new,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toCollection(LinkedList::new))
                ));
        gameResultService.addGeneratedVanillaItems(chatId, weightScale.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                )));
        log.info("Adding effects to generated items, amount: {}", weightScale.size());
        double startSegmentWeight = 0.0;
        int weaponCount = 0;
        int wearableCount = 0;
        val vanillaItemsIds = weightScale.values().stream().flatMap(Collection::stream).toList();
        while (weaponCount < weaponPerGame || wearableCount < wearablesPerGame) {
            val multipleItemsWeight = weightScale.entrySet().stream().filter(e -> e.getValue().size() > 1).toList();
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
                expectedWeightChange = -(initialWeight * weightScale.get(initialWeight).size()) / (largestSegment * largestSegment);
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
                        weightScale.put(initialWeight, oldValueList);
                    }
                    insertNewItem(Pair.create(itemId, updatedWeight.get()), weightScale);
                    switch (itemService.findItem(chatId, itemId).getItemType()) {
                        case WEAPON:
                            weaponCount++;
                        case WEARABLE:
                            wearableCount++;
                    }
                }
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
        return calculateParameters(chatId, weapon);
    }

    private Weapon calculateParameters(long chatId, Weapon weapon) {
        log.info("Calculating weapon params...");
        val defaultAttributes = balanceMatrixService.getBalanceMatrixColumn(chatId, "weapon_type_attr", weapon.getAttributes().getWeaponType().ordinal());
        log.debug("Default attributes: {}", defaultAttributes);
        weapon.setAttack((int) defaultAttributes[0]);
        weapon.setCriticalHitChance(defaultAttributes[1]);
        weapon.setCriticalHitMultiplier(defaultAttributes[2]);
        weapon.setChanceToMiss(defaultAttributes[3]);
        weapon.setChanceToKnockOut(defaultAttributes[4]);
        if (STAFF.equals(weapon.getAttributes().getWeaponType())) {
            weapon.setMagicType(getRandomMagicType());
        }

        val handlingAdjustmentAttributes = balanceMatrixService.getBalanceMatrixColumn(chatId, "weapon_handling_type_adjustment", weapon.getAttributes().getHandling().ordinal());
        log.debug("Handling adjustment attributes: {}", handlingAdjustmentAttributes);
        applyAdjustment(weapon, handlingAdjustmentAttributes);

        val weaponMaterialAdjustmentAttributes = balanceMatrixService.getBalanceMatrixColumn(chatId, "weapon_material_adjustment", weapon.getAttributes().getWeaponMaterial().ordinal());
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

        val handlerMaterial = getEqualWeaponHandlerMaterial(weapon);
        if (handlerMaterial.isPresent()) {
            log.info("Weapon and handler materials matched, {}", handlerMaterial.get());
            val completeMaterialAdjustment =
                    switch (handlerMaterial.get()) {
                        case WOOD ->
                                balanceMatrixService.getBalanceMatrixColumn(chatId, "weapon_complete_wood_adjustment", 0);
                        case STEEL ->
                                balanceMatrixService.getBalanceMatrixColumn(chatId, "weapon_complete_steel_adjustment", 0);
                        case DRAGON_BONE ->
                                balanceMatrixService.getBalanceMatrixColumn(chatId, "weapon_complete_dragon_bone_adjustment", 0);
                        default -> throw new IllegalStateException("Unexpected value: " + handlerMaterial.get());
                    };
            log.debug("Complete material adjustment attributes: {}", completeMaterialAdjustment);
            applyAdjustment(weapon, completeMaterialAdjustment);
        }

        val weaponHandlerAdjustment =
                balanceMatrixService.getBalanceMatrixColumn(chatId, "weapon_handler_material_adjustment", weapon.getAttributes().getWeaponHandlerMaterial().ordinal());
        log.debug("Weapon handler adjustment attributes: {}", weaponHandlerAdjustment);
        applyAdjustment(weapon, weaponHandlerAdjustment);

        val sizeAdjustment = balanceMatrixService.getBalanceMatrixColumn(chatId, "weapon_size_adjustment", weapon.getAttributes().getSize().ordinal());
        log.debug("Size adjustment attributes: {}", sizeAdjustment);
        applyAdjustment(weapon, sizeAdjustment);

        val attackTypeAdjustment =
                balanceMatrixService.getBalanceMatrixRow(chatId, "weapon_attack_type_adjustment", weapon.getAttributes().getWeaponAttackType().ordinal());
        log.debug("Attack type adjustment attributes: {}", attackTypeAdjustment);
        applyAdjustment(weapon, attackTypeAdjustment);
        val qualityAdjustmentRatio =
                balanceMatrixService.getBalanceMatrixRow(chatId, "items_quality_adjustment", weapon.getAttributes().getQuality().ordinal());
        log.debug("Quality adjustment ratio: {}", qualityAdjustmentRatio);
        applyAdjustment(weapon, qualityAdjustmentRatio);
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
        if (WearableType.BOOTS.equals(wearable.getAttributes().getWearableType())) {
            chanceToDodge = balanceMatrixService.getBalanceMatrixValue(wearable.getChatId(), "wearable_chance_to_dodge_adjustment", 0, wearable.getAttributes().getWearableMaterial().ordinal());
        }
        armor += (int) balanceMatrixService.getBalanceMatrixValue(wearable.getChatId(), "wearable_material_adjustment", 0, wearable.getAttributes().getWearableMaterial().ordinal());
        val qualityRatio = balanceMatrixService.getBalanceMatrixValue(wearable.getChatId(), "items_quality_adjustment", 0, wearable.getAttributes().getQuality().ordinal());
        armor = (int) (armor * qualityRatio);
        chanceToDodge *= qualityRatio;
        wearable.setArmor(armor);
        wearable.setChanceToDodge(chanceToDodge);
        return wearable;
    }

    private <T extends Enum> Map<T, Double> generateAttributeMap(double[][] defaultAttributesMatrix, Class<T> attribute) {
        return normalizeMap(Arrays.stream(attribute.getEnumConstants())
                .collect(Collectors.toMap(
                        Function.identity(),
                        e -> {
                            int index = e.ordinal();
                            double sum = 0.0;
                            for (int i = 0; i < weaponAttributeVectorSize; i++) {
                                double value = defaultAttributesMatrix[i][index];
                                if (value != 0.0) {
                                    sum += 1 / value;
                                }
                            }
                            return sum;
                        }
                )));
    }

    private <T extends Enum> Map<T, Double> generateAttributeMap(double[][] defaultAttributesMatrix, T... attributes) {
        return normalizeMap(Arrays.stream(attributes)
                .collect(Collectors.toMap(
                        Function.identity(),
                        e -> {
                            int index = e.ordinal();
                            double sum = 0.0;
                            for (int i = 0; i < weaponAttributeVectorSize; i++) {
                                sum += defaultAttributesMatrix[i][index];
                            }
                            return sum;
                        }
                )));
    }

    private <T> Map<T, Double> normalizeMap(Map<T, Double> map) {
        double sum = map.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum == 0.0) {
            double uniform = 1.0 / map.size();
            return map.keySet().stream()
                    .collect(Collectors.toMap(Function.identity(), k -> uniform));
        }
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() / sum
                ));
    }
}
