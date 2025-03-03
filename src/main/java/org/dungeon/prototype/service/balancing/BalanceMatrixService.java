package org.dungeon.prototype.service.balancing;

import org.dungeon.prototype.repository.postgres.BalanceMatrixRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BalanceMatrixService {

    @Autowired
    private BalanceMatrixRepository balanceMatrixRepository;

    public double[][] getPlayerAttackMatrix(long chatId) {
        return balanceMatrixRepository.getMatrix(chatId, "player_attack");
    }

    public double[][] getPlayerDefenceMatrix(long chatId) {
        return balanceMatrixRepository.getMatrix(chatId, "player_defense");
    }

    public double[][] getMonsterAttackMatrix(long chatId) {
        return balanceMatrixRepository.getMatrix(chatId, "monster_attack");
    }

    public double[][] getMonsterDefenseMatrix(long chatId) {
        return balanceMatrixRepository.getMatrix(chatId, "monster_defense");
    }

}
