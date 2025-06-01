package org.dungeon.prototype.model.level.generation;

import org.dungeon.prototype.model.level.ui.GridSection;


public record GeneratedCluster(long chatId, long clusterId, GridSection[][] clusterGrid) {
}
