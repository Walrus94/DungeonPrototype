package org.dungeon.prototype.service.balancing;

import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.exception.DungeonPrototypeException;
import org.dungeon.prototype.kafka.KafkaProducer;
import org.dungeon.prototype.model.inventory.attributes.Quality;
import org.dungeon.prototype.model.inventory.attributes.weapon.*;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial;
import org.dungeon.prototype.model.kafka.request.balance.BalanceMatrixGenerationRequest;
import org.dungeon.prototype.model.monster.MonsterAttackType;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.repository.postgres.BalanceMatrixRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@Service
public class BalanceMatrixService {
    @Value("${generation.items.weapon.weapon-attribute-vector-size}")
    private int weaponAttributeVectorSize;

    @Autowired
    KafkaProducer kafkaProducer;
    @Autowired
    private BalanceMatrixRepository balanceMatrixRepository;

    public void initializeBalanceMatrices(long chatId) {
        log.info("Initializing balance matrices for chatId: {}", chatId);
        initializeBalanceMatrix(chatId, "weapon_type_attr", weaponAttributeVectorSize, WeaponType.values().length);
        initializeBalanceMatrix(chatId, "weapon_handling_type_adjustment", weaponAttributeVectorSize, Handling.values().length);
        initializeBalanceMatrix(chatId, "weapon_material_adjustment", weaponAttributeVectorSize, WeaponMaterial.values().length);
        initializeBalanceMatrix(chatId, "weapon_handler_material_adjustment", weaponAttributeVectorSize, WeaponHandlerMaterial.values().length);
        initializeBalanceMatrix(chatId, "weapon_complete_wood_adjustment", weaponAttributeVectorSize, 1);
        initializeBalanceMatrix(chatId, "weapon_complete_steel_adjustment", weaponAttributeVectorSize, 1);
        initializeBalanceMatrix(chatId, "weapon_complete_dragon_bone_adjustment", weaponAttributeVectorSize, 1);
        initializeBalanceMatrix(chatId, "weapon_size_adjustment", weaponAttributeVectorSize, Size.values().length);
        initializeBalanceMatrix(chatId, "weapon_attack_type_adjustment", weaponAttributeVectorSize, WeaponAttackType.values().length);
        initializeBalanceMatrix(chatId, "items_quality_adjustment", Quality.values().length, 1);
        initializeBalanceMatrix(chatId, "player_attack", WeaponAttackType.values().length, MonsterClass.values().length);
        initializeBalanceMatrix(chatId, "monster_attack", MonsterAttackType.values().length, WearableMaterial.values().length);
    }

    public double getBalanceMatrixValue(long chatId, String name, int row, int col) {
        while (!balanceMatrixRepository.isTableExists(chatId, name)) {
            log.info("Waiting for balance matrix {} table to be created for chatId: {}", name, chatId);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DungeonPrototypeException(e.getMessage());
            }
        }
        return balanceMatrixRepository.getValue(chatId, name, row, col);
    }

    public double[][] getBalanceMatrix(long chatId, String name) {
        while (!balanceMatrixRepository.isTableExists(chatId, name)) {
            log.info("Waiting for balance matrix {} table to be created for chatId: {}", name, chatId);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DungeonPrototypeException(e.getMessage());
            }
        }
        return balanceMatrixRepository.getBalanceMatrix(chatId,  name);
    }

    public double[] getBalanceVector(long chatId, String name, int col) {
        return Arrays.stream(getBalanceMatrix(chatId, name))
                .mapToDouble(row -> row[col])
                .toArray();
    }

    private void initializeBalanceMatrix(long chatId, String name, int rows, int cols) {
        kafkaProducer.sendBalanceMatrixGenerationRequest(
                new BalanceMatrixGenerationRequest(chatId, name, cols, rows)
        );
    }

    public void clearMatrices(long chatId) {
        balanceMatrixRepository.clearBalanceMatrix(chatId, "player_attack");
        balanceMatrixRepository.clearBalanceMatrix(chatId, "monster_attack");
        balanceMatrixRepository.clearBalanceMatrix(chatId, "weapon_type_attr");
        balanceMatrixRepository.clearBalanceMatrix(chatId, "weapon_handling_type_adjustment");
        balanceMatrixRepository.clearBalanceMatrix(chatId, "weapon_material_adjustment");
        balanceMatrixRepository.clearBalanceMatrix(chatId, "weapon_handler_material_adjustment");
        balanceMatrixRepository.clearBalanceMatrix(chatId, "weapon_complete_wood_adjustment");
        balanceMatrixRepository.clearBalanceMatrix(chatId, "weapon_complete_steel_adjustment");
        balanceMatrixRepository.clearBalanceMatrix(chatId, "weapon_complete_dragon_bone_adjustment");
        balanceMatrixRepository.clearBalanceMatrix(chatId, "weapon_size_adjustment");
        balanceMatrixRepository.clearBalanceMatrix(chatId, "weapon_attack_type_adjustment");
        balanceMatrixRepository.clearBalanceMatrix(chatId, "items_quality_adjustment");
    }

    public Map<String, Double[][]> getAllMatrices(long chatId) {
        return balanceMatrixRepository.getAllMatrices(chatId);
    }
}
