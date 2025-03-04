package org.dungeon.prototype.repository.mongo.projections;

import lombok.Data;

@Data
public class ItemWeightProjection {
    private String id;
    private Double weightAbs;
}
