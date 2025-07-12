package org.dungeon.prototype.service.balancing;

import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.exception.DungeonPrototypeException;
import org.dungeon.prototype.kafka.KafkaProducer;
import org.dungeon.prototype.model.inventory.attributes.Quality;
import org.dungeon.prototype.model.inventory.attributes.weapon.*;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial;
import org.dungeon.prototype.model.kafka.request.balance.BalanceMatricesRequest;
import org.dungeon.prototype.model.kafka.request.balance.BalanceMatrixGenerationRequest;
import org.dungeon.prototype.model.monster.MonsterAttackType;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.repository.mongo.BalanceMatrixRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

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
        initializeBalanceMatrix(chatId, List.of(
                new BalanceMatrixGenerationRequest("weapon_type_attr", weaponAttributeVectorSize, WeaponType.values().length),
                new BalanceMatrixGenerationRequest("weapon_handling_type_adjustment", weaponAttributeVectorSize, Handling.values().length),
                new BalanceMatrixGenerationRequest("weapon_material_adjustment", weaponAttributeVectorSize, WeaponMaterial.values().length),
                new BalanceMatrixGenerationRequest("weapon_handler_material_adjustment", weaponAttributeVectorSize, WeaponHandlerMaterial.values().length),
                new BalanceMatrixGenerationRequest("weapon_complete_wood_adjustment", weaponAttributeVectorSize, 1),
                new BalanceMatrixGenerationRequest("weapon_complete_steel_adjustment", weaponAttributeVectorSize, 1),
                new BalanceMatrixGenerationRequest("weapon_complete_dragon_bone_adjustment", weaponAttributeVectorSize, 1),
                new BalanceMatrixGenerationRequest("items_size_adjustment", weaponAttributeVectorSize, Size.values().length),
                new BalanceMatrixGenerationRequest("weapon_attack_type_adjustment", weaponAttributeVectorSize, WeaponAttackType.values().length),
                new BalanceMatrixGenerationRequest("items_quality_adjustment", Quality.values().length, 1),
                new BalanceMatrixGenerationRequest("wearable_armor_bonus", WearableMaterial.values().length, 1),
                new BalanceMatrixGenerationRequest("wearable_chance_to_dodge_adjustment", WearableMaterial.values().length, 1),
                new BalanceMatrixGenerationRequest("player_attack", WeaponAttackType.values().length, MonsterClass.values().length),
                new BalanceMatrixGenerationRequest("monster_attack", MonsterAttackType.values().length, WearableMaterial.values().length)
        ));
    }

    public double getBalanceMatrixValue(long chatId, String name, int row, int col) {
        while (!balanceMatrixRepository.existsByChatIdAndName(chatId, name)) {
            log.info("Waiting for balance matrix {} table to be created for chatId: {}", name, chatId);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DungeonPrototypeException(e.getMessage());
            }
        }
        return balanceMatrixRepository.getMatrixCellValue(chatId, name, row, col).orElseThrow(
                () -> new DungeonPrototypeException("Balance matrix value not found for chatId: " + chatId + ", name: " + name + ", row: " + row + ", col: " + col)
        ).getValue();
    }

    public double[][] getBalanceMatrix(long chatId, String name) {
        while (!balanceMatrixRepository.existsByChatIdAndName(chatId, name)) {
            log.info("Waiting for balance matrix {} table to be created for chatId: {}", name, chatId);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DungeonPrototypeException(e.getMessage());
            }
        }
        return convertMatrix(balanceMatrixRepository.findByChatIdAndName(chatId, name)
                .orElseThrow(() ->
                        new DungeonPrototypeException("Unable to find balance matrix for chatId: " + chatId + ", name: " + name))
                .getData());
    }

    public double[] getBalanceMatrixColumnVector(long chatId, String name, int column) {
        double[][] matrix = getBalanceMatrix(chatId, name);
        if (matrix.length == 0) {
            throw new DungeonPrototypeException("Empty balance matrix for " + name);
        }
        if (column < matrix[0].length) {
            return Arrays.stream(matrix)
                    .mapToDouble(row -> row[column])
                    .toArray();
        }
        if (column < matrix.length) { // handle reversed orientation
            return matrix[column];
        }
        throw new DungeonPrototypeException("Index out of bounds for balance matrix " + name + ": " + column);
    }

    public double[] getBalanceMatrixRowVector(long chatId, String name, int row) {
        double[][] matrix = getBalanceMatrix(chatId, name);
        if (matrix.length == 0) {
            throw new DungeonPrototypeException("Empty balance matrix for " + name);
        }
        if (row < matrix.length) {
            return matrix[row];
        }
        if (row < matrix[0].length) { // handle reversed orientation
            return Arrays.stream(matrix)
                    .mapToDouble(r -> r[row])
                    .toArray();
        }
        throw new DungeonPrototypeException("Index out of bounds for balance matrix " + name + ": " + row);
    }

    private void initializeBalanceMatrix(long chatId, List<BalanceMatrixGenerationRequest> requests) {
        kafkaProducer.sendBalanceMatrixGenerationRequest(
                new BalanceMatricesRequest(chatId, requests)
        );
    }

    public void clearMatrices(long chatId) {
        balanceMatrixRepository.deleteAllByChatId(chatId);
    }

    private double[][] convertMatrix(List<List<Double>> matrix) {
        return matrix.stream()
                .map(row -> row.stream().mapToDouble(Double::doubleValue).toArray())
                .toArray(double[][]::new);
    }
}
