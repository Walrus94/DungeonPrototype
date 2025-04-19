package org.dungeon.prototype.service.stats;

import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.stats.GameResult;
import org.dungeon.prototype.model.stats.MonsterData;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.repository.mongo.GameResultRepository;
import org.dungeon.prototype.repository.mongo.converters.mapstruct.GameResultMapper;
import org.dungeon.prototype.service.balancing.BalanceMatrixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameResultService {
    @Autowired
    BalanceMatrixService balanceMatrixService;
    @Autowired
    private GameResultRepository gameResultRepository;
    private final Map<Long, GameResult> gameResultMap = new ConcurrentHashMap<>();
    private final Map<Long, MonsterData> currentMonster = new ConcurrentHashMap<>();

    public void addPlayerStep(long chatId, Weight weight) {
        GameResult gameResult = gameResultMap.computeIfAbsent(chatId, k -> new GameResult(chatId));
        if (gameResult.addWeight(weight)) {
            gameResultMap.put(chatId, gameResult);
        }
    }

    public void addGeneratedVanillaItems(long chatId, Map<Double, Integer> itemsWeightScale) {
        GameResult gameResult = gameResultMap.get(chatId);

        gameResult.setVanillaItemsWeightScale(itemsWeightScale);

        gameResultMap.put(chatId, gameResult);
    }

    public void addCurrentMonster(long chatId, MonsterClass monsterClass, Weight weight) {
        currentMonster.put(chatId, new MonsterData(monsterClass, weight));
    }

    public void incrementBattleStep(long chatId) {
        var monster = currentMonster.get(chatId);
        monster.incrementSteps();
        currentMonster.put(chatId, monster);
    }

    public void saveDefeatedMonster(long chatId) {
        MonsterData monsterData = currentMonster.remove(chatId);
        GameResult gameResult = gameResultMap.get(chatId);

        monsterData.setStepKilled(gameResult.getPlayerWeightDynamic().size());
        if (gameResult.addDefeatedMonster(monsterData)) {
            gameResultMap.put(chatId, gameResult);
        }

    }

    public void dungeonLevelReached(long chatId) {
        GameResult gameResult = gameResultMap.get(chatId);

        if (gameResult.addDungeonLevelReached(gameResult.getPlayerWeightDynamic().size())) {
            gameResultMap.put(chatId, gameResult);
        }
    }

    public void playerLevelReached(long chatId) {
        GameResult gameResult = gameResultMap.get(chatId);

        if (gameResult.addPlayerLevelReached(gameResult.getPlayerWeightDynamic().size())) {
            gameResultMap.put(chatId, gameResult);
        }
    }

    public void registerDeathAndSaveResult(long chatId) {
        GameResult gameResult = gameResultMap.remove(chatId);

        gameResult.setDeath(true);
        gameResult.setBalanceMatrices(collectBalanceMatrices(chatId));

        gameResultRepository.save(GameResultMapper.INSTANCE.mapToDocument(gameResult));

    }

    private Map<String, Double[][]> collectBalanceMatrices(long chatId) {
        return balanceMatrixService.getAllMatrices(chatId);
    }
}
