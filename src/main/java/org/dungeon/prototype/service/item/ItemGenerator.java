package org.dungeon.prototype.service.item;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.dungeon.prototype.model.inventory.attributes.Quality;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponHandlerMaterial;
import org.dungeon.prototype.model.inventory.attributes.weapon.Handling;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponMaterial;
import org.dungeon.prototype.model.inventory.attributes.weapon.Size;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponType;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.properties.GenerationProperties;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.effect.ItemEffectsGenerator;
import org.dungeon.prototype.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.BLUNT;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.SLASH;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.STAB;
import static org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType.STRIKE;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Handling.SINGLE_HANDED;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Handling.TWO_HANDED;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Size.LARGE;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Size.MEDIUM;
import static org.dungeon.prototype.model.inventory.attributes.weapon.Size.SMALL;
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
import static org.dungeon.prototype.util.GenerationUtil.*;
import static org.dungeon.prototype.util.RandomUtil.getRandomEnumValue;
import static org.dungeon.prototype.util.RandomUtil.getRandomInt;

@Slf4j
@Component
public class ItemGenerator {
    @Value("${generation.items.weapon.weapon-attributes-pool-size}")
    private Integer weaponAttributesPoolSize;
    @Value("${generation.items.weapon.weapon-per-game}")
    private Integer weaponPerGame;
    @Value("${generation.items.wearables.wearables-per-game}")
    private Integer wearablesPerGame;
    @Value("${generation.items.wearables.default-armor}")
    private Integer defaultArmor;
    @Value("${generation.items.wearables.wearable-min-effects-play}")
    private Integer wearableMinEffectsPlay;
    @Value("${generation.items.wearables.wearable-max-effects-play}")
    private Integer wearableMaxEffectsPlay;
    @Value("${generation.items.selling-price-ratio}")
    private double sellingPriceRatio;
    @Value("${generation.items.buying-price-ratio}")
    private double buyingPriceRatio;
    @Autowired
    private ItemEffectsGenerator itemEffectsGenerator;
    @Autowired
    private EffectService effectService;
    @Autowired
    private GenerationProperties generationProperties;

    @Transactional
    public Set<Weapon> generateWeapons(Long chatId) {
        Set<Weapon> totalWeapons = new HashSet<>();
        Set<WeaponAttributes> generatedAttributesCombinations = new HashSet<>();
        while (generatedAttributesCombinations.size() < weaponAttributesPoolSize) {
            val type = getRandomEnumValue(List.of(WeaponType.values()));
            log.debug("Weapon type: {}", type);
            Handling handling;
            switch (type) {
                case SWORD, AXE -> handling = getRandomEnumValue(List.of(SINGLE_HANDED, TWO_HANDED));
                case DAGGER -> handling = SINGLE_HANDED;
                default -> handling = getRandomEnumValue(List.of(Handling.values())); //TODO: consider more constraints
            }
            log.debug("Weapon handling: {}", handling);
            WeaponMaterial weaponMaterial;
            switch (type) {
                case SWORD, AXE, DAGGER -> weaponMaterial = getRandomEnumValue(List.of(WeaponMaterial.IRON, WeaponMaterial.STEEL, PLATINUM, DIAMOND, WeaponMaterial.MITHRIL, DRAGON_BONE));
                case CLUB -> weaponMaterial = getRandomEnumValue(List.of(WOOD, STONE, ENCHANTED_WOOD));
                case MACE -> weaponMaterial = getRandomEnumValue(List.of(WeaponMaterial.STONE, WeaponMaterial.IRON, WeaponMaterial.STEEL, PLATINUM, DIAMOND, WeaponMaterial.MITHRIL, OBSIDIAN, DRAGON_BONE, ENCHANTED_WOOD));
                case STAFF -> weaponMaterial = getRandomEnumValue(List.of(WOOD, STONE, OBSIDIAN, WeaponMaterial.MITHRIL, DRAGON_BONE, ENCHANTED_WOOD));
                default -> weaponMaterial = getRandomEnumValue(List.of(WeaponMaterial.values()));
            }
            log.debug("Weapon material: {}", weaponMaterial);
            WeaponHandlerMaterial weaponHandlerMaterial;
            switch (weaponMaterial) {
                case WOOD -> weaponHandlerMaterial = getRandomEnumValue(List.of(WeaponHandlerMaterial.WOOD, WeaponHandlerMaterial.LEATHER, TREATED_LEATHER));
                case STONE -> weaponHandlerMaterial = getRandomEnumValue(List.of(WeaponHandlerMaterial.LEATHER, TREATED_LEATHER));
                case ENCHANTED_WOOD -> weaponHandlerMaterial = getRandomEnumValue(List.of(WeaponHandlerMaterial.WOOD, LEATHER, TREATED_LEATHER));
                default -> weaponHandlerMaterial = getRandomEnumValue(List.of(WeaponHandlerMaterial.values()));
            }
            log.debug("Weapon handler material: {}", weaponHandlerMaterial);
            Quality quality;
            if (DRAGON_BONE.equals(weaponMaterial) && WeaponHandlerMaterial.DRAGON_BONE.equals(weaponHandlerMaterial)) {
                quality = Quality.MYTHIC;
            } else {
                quality = getRandomEnumValue(List.of(Quality.values()));
            }
            log.debug("Weapon quality: {}", quality);
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
            log.debug("Weapon size: {}", size);
            WeaponAttackType weaponAttackType;
            switch (type) {
                case SWORD -> weaponAttackType = getRandomEnumValue(List.of(STAB, SLASH));
                case AXE -> weaponAttackType = SLASH;
                case DAGGER, SPEAR -> weaponAttackType = STAB;
                case CLUB -> weaponAttackType = BLUNT;
                case MACE -> weaponAttackType = getRandomEnumValue(List.of(SLASH, BLUNT));
                default -> weaponAttackType = STRIKE;
            }
            log.debug("Weapon attack type: {}", weaponAttackType);
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
        val weapons = generatedAttributesCombinations.stream()
                .map(weaponAttributes -> generateWeapon(weaponAttributes, chatId))
                .toList();
        RandomUtil.getRandomWeaponsStream(weapons.stream()
                .map(weapon -> {
                    val probability = (double) (weapon.getWeight());
                    return new Pair<>(weapon, probability);
                }).toList(), weaponPerGame).forEach(weapon -> {
                    log.debug("Generated weapon: {}", weapon);
                    totalWeapons.add(weapon);
                });
        return totalWeapons;
    }

    @Transactional
    public Set<Wearable> generateWearables(Long chatId) {
        Set<WearableAttributes> attributesCombinations = new HashSet<>();
        Map<WearableType, Integer> typesCount = new HashMap<>();
        while (attributesCombinations.size() < wearablesPerGame) {
            var type = getRandomEnumValue(List.of(WearableType.values()));
            WearableMaterial material = null;
            switch (type) {
                case VEST -> {
                    material = RandomUtil.getRandomEnumValue(List.of(WearableMaterial.values()), List.of(ELVEN_SILK, WOOL));
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
        return attributesCombinations.stream()
                .flatMap(attributes -> Stream.concat(Stream.of(generateVanillaWearable(attributes, chatId)),
                        Stream.generate(() -> generateWearable(attributes, chatId))
                                .limit(getRandomInt(wearableMinEffectsPlay, wearableMaxEffectsPlay))))
                .peek(wearable -> log.debug("Generated wearable: {}", wearable))
                .collect(Collectors.toSet());
    }

    private Weapon generateWeapon(WeaponAttributes weaponAttributes, Long chatId) {
        log.debug("Generating weapon...");
        val weapon = new Weapon();
        weapon.setAttributes(weaponAttributes);
        weapon.setChatId(chatId);
        weapon.setEffects(effectService.saveItemEffects(itemEffectsGenerator.generateEffects()));
        return calculateParameters(weapon);
    }

    private Weapon calculateParameters(Weapon weapon) {
        log.debug("Calculating weapon params...");
        val properties = generationProperties.getItems().getWeapon();
        val defaultAttributesMap = properties.getDefaultAttributes();
        val defaultAttributes = defaultAttributesMap.get(weapon.getAttributes().getWeaponType());
        weapon.setAttack(defaultAttributes.getAttack());
        weapon.setCriticalHitChance(defaultAttributes.getCriticalHitChance());
        weapon.setChanceToMiss(defaultAttributes.getChanceToMiss());
        weapon.setChanceToKnockOut(defaultAttributes.getChanceToKnockOut());
        if (STAFF.equals(weapon.getAttributes().getWeaponType())) {
            weapon.setHasMagic(true);
            weapon.setMagicType(MagicType.values()[getRandomInt(0, MagicType.values().length - 1)]);
        }

        val handlingAdjustmentAttributes = properties.getHandlingAdjustmentAttributes().get(weapon.getAttributes().getHandling());
        weapon.setAttack((int) (weapon.getAttack() * handlingAdjustmentAttributes.getAttackRatio()));
        weapon.setChanceToMiss(weapon.getChanceToMiss() * handlingAdjustmentAttributes.getChanceToMissRatio());
        weapon.setCriticalHitChance(weapon.getCriticalHitChance() * handlingAdjustmentAttributes.getCriticalChanceRatio());

        val weaponMaterialAdjustmentAttributes = properties.getWeaponMaterialAdjustmentAttributes().get(weapon.getAttributes().getWeaponMaterial());
        weapon.setAttack((int) (weapon.getAttack() * weaponMaterialAdjustmentAttributes.getAttackRatio()));
        weapon.setChanceToMiss(weapon.getChanceToMiss() * weaponMaterialAdjustmentAttributes.getChanceToMissRatio());
        weapon.setCriticalHitChance(weapon.getCriticalHitChance() * weaponMaterialAdjustmentAttributes.getCriticalChanceRatio());
        weapon.setChanceToKnockOut(weapon.getChanceToKnockOut() * weaponMaterialAdjustmentAttributes.getKnockOutChanceRatio());
        if (ENCHANTED_WOOD.equals(weapon.getAttributes().getWeaponMaterial()) && (isNull(weapon.getHasMagic()) || !weapon.getHasMagic())) {
            weapon.setHasMagic(true);
            weapon.setMagicType(MagicType.values()[getRandomInt(0, MagicType.values().length - 1)]);
        }
        if (DRAGON_BONE.equals(weapon.getAttributes().getWeaponMaterial()) && WeaponHandlerMaterial.DRAGON_BONE.equals(weapon.getAttributes().getWeaponHandlerMaterial())) {
            weapon.setIsCompleteDragonBone(true);
            weapon.setHasMagic(true);
            weapon.setMagicType(MagicType.CHAOTIC);//TODO: add magic coordinates to store type
        } else {
            weapon.setIsCompleteDragonBone(false);
        }

        val completeMaterialAdjustmentAttributes= properties.getCompleteMaterialAdjustmentAttributes();
        val handlerMaterial = getEqualWeaponHandlerMaterial(weapon);
        if (handlerMaterial.isPresent() && completeMaterialAdjustmentAttributes.containsKey(handlerMaterial.get())) {
            val completeMaterialAdjustment = completeMaterialAdjustmentAttributes.get(handlerMaterial.get());
            weapon.setAttack((int) (weapon.getAttack() * completeMaterialAdjustment.getAttackRatio()));
            weapon.setChanceToMiss(weapon.getChanceToMiss() * completeMaterialAdjustment.getChanceToMissRatio());
            weapon.setCriticalHitChance(weapon.getCriticalHitChance() * completeMaterialAdjustment.getCriticalChanceRatio());
            weapon.setChanceToKnockOut(weapon.getChanceToKnockOut() * completeMaterialAdjustment.getKnockOutChanceRatio());
        } else {
            val weaponHandlerAdjustment = properties.getWeaponHandlerMaterialAdjustmentAttributes().get(weapon.getAttributes().getWeaponHandlerMaterial());
            weapon.setAttack((int) (weapon.getAttack() * weaponHandlerAdjustment.getAttackRatio()));
            weapon.setChanceToMiss(weapon.getChanceToMiss() * weaponHandlerAdjustment.getChanceToMissRatio());
            weapon.setCriticalHitChance(weapon.getCriticalHitChance() * weaponHandlerAdjustment.getCriticalChanceRatio());
            weapon.setChanceToKnockOut(weapon.getChanceToKnockOut() * weaponHandlerAdjustment.getKnockOutChanceRatio());
        }
        val sizeAdjustment = properties.getSizeAdjustmentAttributes().get(weapon.getAttributes().getSize());
        weapon.setAttack((int) (weapon.getAttack() * sizeAdjustment.getAttackRatio()));
        weapon.setChanceToMiss(weapon.getChanceToMiss() * sizeAdjustment.getChanceToMissRatio());

        val attackTypeAdjustment = properties.getAttackTypeAdjustmentAttributes().get(weapon.getAttributes().getWeaponAttackType());
        weapon.setAttack((int) (weapon.getAttack() * attackTypeAdjustment.getAttackRatio()));
        weapon.setCriticalHitChance(weapon.getCriticalHitChance() * attackTypeAdjustment.getCriticalChanceRatio());
        weapon.setChanceToKnockOut(weapon.getChanceToKnockOut() * attackTypeAdjustment.getKnockOutChanceRatio());
        if (STRIKE.equals(weapon.getAttributes().getWeaponAttackType())) {
            if (isNull(weapon.getHasMagic()) || !weapon.getHasMagic()) {
                weapon.setHasMagic(true);
                weapon.setMagicType(MagicType.values()[getRandomInt(0, MagicType.values().length - 1)]);
            }
        }
        val qualityAdjustmentRatio = properties.getQualityAdjustmentRatio().get(weapon.getAttributes().getQuality());
        multiplyAllParametersBy(weapon, qualityAdjustmentRatio);
        weapon.setWeight(calculateWeight(weapon));
        weapon.setSellingPrice((int) (weapon.getWeight() * sellingPriceRatio));
        weapon.setBuyingPrice((int) (weapon.getWeight() * buyingPriceRatio));
        return weapon;
    }

    @NotNull
    private static Optional<WeaponHandlerMaterial> getEqualWeaponHandlerMaterial(Weapon weapon) {
        try {
            val weaponHandlerMaterial = WeaponHandlerMaterial.valueOf(weapon.getAttributes().getWeaponMaterial().toString());
            return Optional.of(weaponHandlerMaterial);

        } catch (Exception e) {
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

    private Wearable generateWearable(WearableAttributes wearableAttributes, Long chatId) {
        val wearable = new Wearable();
        wearable.setAttributes(wearableAttributes);
        wearable.setChatId(chatId);
        wearable.setEffects(effectService.saveItemEffects(itemEffectsGenerator.generateEffects()));
        return calculateParameters(wearable);
    }

    private Wearable calculateParameters(Wearable wearable) {
        if (wearable.getAttributes().getWearableMaterial().equals(WearableMaterial.ENCHANTED_LEATHER)) {
            wearable.setHasMagic(true);
            wearable.setMagicType(MagicType.values()[getRandomInt(0, MagicType.values().length - 1)]);
        }
        var armor = defaultArmor;
        var chanceToDodge = 0.0;
        val wearablesProperties = generationProperties.getItems().getWearables();
        if (WearableType.BOOTS.equals(wearable.getAttributes().getWearableType())) {
            chanceToDodge = wearablesProperties.getChanceToDodgeRatio().get(wearable.getAttributes().getWearableMaterial());
        }
        armor += wearablesProperties.getArmorBonus().get(wearable.getAttributes().getWearableMaterial());
        val qualityRatio = wearablesProperties.getQualityAdjustmentRatio().get(wearable.getAttributes().getQuality());
        armor = (int) (armor *  qualityRatio);
        chanceToDodge *= qualityRatio;
        wearable.setArmor(armor);
        wearable.setChanceToDodge(chanceToDodge);
        wearable.setWeight(armor * 100 + wearable.getEffects().stream().mapToInt(Effect::getWeight).sum());

        wearable.setSellingPrice((int) (wearable.getWeight() * sellingPriceRatio));
        wearable.setBuyingPrice((int) (wearable.getWeight() * buyingPriceRatio));
        return wearable;
    }
}
