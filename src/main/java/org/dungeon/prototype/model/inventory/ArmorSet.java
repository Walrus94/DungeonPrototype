package org.dungeon.prototype.model.inventory;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.inventory.items.Boots;
import org.dungeon.prototype.model.inventory.items.Gloves;
import org.dungeon.prototype.model.inventory.items.Helmet;
import org.dungeon.prototype.model.inventory.items.Vest;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ArmorSet {
    private Helmet helmet;
    private Vest vest;
    private Gloves gloves;
    private Boots boots;

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
