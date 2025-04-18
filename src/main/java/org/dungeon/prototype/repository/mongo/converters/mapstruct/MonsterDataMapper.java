package org.dungeon.prototype.repository.mongo.converters.mapstruct;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.dungeon.prototype.model.document.stats.MonsterDataDocument;
import org.dungeon.prototype.model.stats.MonsterData;
import org.dungeon.prototype.model.weight.Weight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;

@Mapper(uses = WeightMapper.class)
public interface MonsterDataMapper {
    @Mapping(target = "weight", qualifiedByName = "fromVector")
    MonsterData mapToEntity(MonsterDataDocument document);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "weight", qualifiedByName = "toVector")
    })
    MonsterDataDocument mapToDocument(MonsterData monsterData);

    @Named("toVector")
    default List<Double> toVector(Weight weight) {
        List<Double> result = new ArrayList<>();

        double[] vector = weight.toVector().toArray();
        for (double d : vector) {
            result.add(d);
        }
        return result;
    }

    @Named("fromVector")
    default Weight fromVector(List<Double> values) {
        double[] array = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return Weight.fromVector(new ArrayRealVector(array));
    }
}
