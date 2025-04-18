package org.dungeon.prototype.model.document.stats;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "game_result")
public class GameResultDocument {
    @Id
    private String Id;
    private Long chatId;
    private boolean death;
    private MonsterDataDocument killer;

    private List<List<Double>> playerWeightDynamic;
    private List<Integer> playerLevelProgression;
    private List<Integer> dungeonLevelProgression;
    private List<MonsterDataDocument> defeatedMonsters;
}
