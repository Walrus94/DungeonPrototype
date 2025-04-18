package org.dungeon.prototype.model.document.stats;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "monster_data")
public class MonsterDataDocument {
    @Id
    private String id;
    private MonsterClass monsterClass;
    private List<Double> weight;
    private int stepKilled;
    private int battleSteps;
}
