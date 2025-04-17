package org.dungeon.prototype.service.balancing;

import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.exception.DungeonPrototypeException;
import org.dungeon.prototype.kafka.KafkaProducer;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableMaterial;
import org.dungeon.prototype.model.kafka.request.balance.BalanceMatrixGenerationRequest;
import org.dungeon.prototype.model.monster.MonsterAttackType;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.repository.postgres.BalanceMatrixRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BalanceMatrixService {
    @Value("${spring.datasource.name}")
    private String dataBase;

    @Autowired
    KafkaProducer kafkaProducer;
    @Autowired
    private BalanceMatrixRepository balanceMatrixRepository;

    public void initializeBalanceMatrices(long chatId) {
        log.info("Initializing balance matrices for chatId: {}", chatId);
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

    private void initializeBalanceMatrix(long chatId, String name, int rows, int cols) {
        kafkaProducer.sendBalanceMatrixGenerationRequest(
                new BalanceMatrixGenerationRequest(chatId, name, cols, rows, dataBase)
        );
    }
}
