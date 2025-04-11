package org.dungeon.prototype.service.balancing;

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

@Service
public class BalanceMatrixService {
    @Value("${spring.datasource.name}")
    private String dataBase;

    @Autowired
    KafkaProducer kafkaProducer;
    @Autowired
    private BalanceMatrixRepository balanceMatrixRepository;

    public double[][] getPlayerAttackMatrix(long chatId) {
        return getOrInitializeMatrix(chatId, "player_attack", WeaponAttackType.values().length, MonsterClass.values().length);
    }

    public double[][] getPlayerDefenceMatrix(long chatId) {
        return getOrInitializeMatrix(chatId, "player_defense", MonsterClass.values().length, WearableMaterial.values().length);
    }

    public double[][] getMonsterAttackMatrix(long chatId) {
        return getOrInitializeMatrix(chatId, "monster_attack", MonsterAttackType.values().length, WearableMaterial.values().length);
    }

    public double[][] getMonsterDefenseMatrix(long chatId) {
        return getOrInitializeMatrix(chatId, "monster_defense", MonsterClass.values().length, WeaponAttackType.values().length);
    }

    private double[][] getOrInitializeMatrix(long chatId, String name, int rows, int cols) {
        double[][] matrix = balanceMatrixRepository.getMatrix(chatId, name);
        if (matrix == null) {
            kafkaProducer.sendBalanceMatrixGenerationRequest(
                    new BalanceMatrixGenerationRequest(chatId, name, cols, rows, dataBase)
            );
        }
        while (matrix == null) {
            matrix = balanceMatrixRepository.getMatrix(chatId, name);
        }
        return matrix;
    }

}
