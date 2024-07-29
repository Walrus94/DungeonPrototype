package org.dungeon.prototype.model.inventory;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.monster.MonsterAttackType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ArmorSet {
    private String id;
    private Wearable helmet;
    private Wearable vest;
    private Wearable gloves;
    private Wearable boots;

    private Map<MonsterAttackType, Double> attackTypeResistanceMap;

    public List<Wearable> getArmorItems() {
        var result = new ArrayList<Wearable>();
        if (helmet != null) {
            result.add(helmet);
        }
        if (vest != null) {
            result.add(vest);
        }
        if (gloves != null) {
            result.add(gloves);
        }
        if (boots != null) {
            result.add(boots);
        }
        return result;
    }
}
