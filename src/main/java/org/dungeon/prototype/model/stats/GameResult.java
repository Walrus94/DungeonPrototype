package org.dungeon.prototype.model.stats;

import lombok.Data;
import org.dungeon.prototype.model.weight.Weight;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class GameResult {
    private long chatId;
    private boolean death;
    private MonsterData killer;
    private List<Weight> playerWeightDynamic;
    private List<Integer> playerLevelProgression;
    private List<Integer> dungeonLevelProgression;
    private List<MonsterData> defeatedMonsters;

    private Map<String, Double[][]> balanceMatrices;

    public GameResult(long chatId) {
        this.chatId = chatId;
        this.death = false;
        this.killer = null;
        this.playerWeightDynamic = new ArrayList<>();
        this.defeatedMonsters = new ArrayList<>();
    }

    public boolean addWeight(Weight weight) {
        return playerWeightDynamic.add(weight);
    }

    public boolean addDefeatedMonster(MonsterData monsterData) {
        return defeatedMonsters.add(monsterData);
    }

    public boolean addDungeonLevelReached(int step) {
        return dungeonLevelProgression.add(step);
    }

    public boolean addPlayerLevelReached(int step) {
        return playerLevelProgression.add(step);
    }
}
