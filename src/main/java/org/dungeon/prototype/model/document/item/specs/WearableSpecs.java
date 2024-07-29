package org.dungeon.prototype.model.document.item.specs;

import lombok.Data;
import org.dungeon.prototype.model.document.item.ItemSpecs;

@Data
public class WearableSpecs implements ItemSpecs {
    private Integer armor;
    private Double chanceToDodge;
}
