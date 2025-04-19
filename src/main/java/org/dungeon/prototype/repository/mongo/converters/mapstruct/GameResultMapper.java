package org.dungeon.prototype.repository.mongo.converters.mapstruct;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.dungeon.prototype.model.document.stats.GameResultDocument;
import org.dungeon.prototype.model.stats.GameResult;
import org.dungeon.prototype.model.weight.Weight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(uses = {WeightMapper.class,
        MonsterDataMapper.class
})
public interface GameResultMapper {
    GameResultMapper INSTANCE = Mappers.getMapper(GameResultMapper.class);
    @Mappings({
            @Mapping(target = "balanceMatrices", qualifiedByName = "toArray"),
            @Mapping(target = "playerWeightDynamic", qualifiedByName = "fromVectors")
    })
    GameResult mapToEntity(GameResultDocument document);
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "balanceMatrices", qualifiedByName = "toList"),
            @Mapping(target = "playerWeightDynamic", qualifiedByName = "toVectors")
    })
    GameResultDocument mapToDocument(GameResult gameResult);

    @Named("toVectors")
    default List<List<Double>> toVectors(List<Weight> weightList) {
        List<List<Double>> result = new ArrayList<>();
        weightList.forEach(weight -> {
            double[] vector = weight.toVector().toArray();
            List<Double> row = new ArrayList<>();
            for (double d: vector) {
                row.add(d);
            }
            result.add(row);
        });
        return result;
    }

    @Named("fromVectors")
    default List<Weight> fromVectors(List<List<Double>> vectors) {
        List<Weight> result = new ArrayList<>();
        vectors.forEach(vector -> {
            double[] array = new double[vector.size()];
            for (int i = 0; i < vector.size(); i++) {
                array[i] = vector.get(i);
            }
            result.add(Weight.fromVector(new ArrayRealVector(array)));
        });
        return result;
    }

    @Named("toList")
    default Map<String, List<List<Double>>> toList(Map<String, Double[][]> value) {
        return value.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<List<Double>> result = new ArrayList<>();
                    for (int i = 0; i < entry.getValue().length; i++) {
                        List<Double> row = new ArrayList<>(Arrays.asList(entry.getValue()[i]));
                        result.add(row);
                    }
                    return result;
                }
        ));
    }

    @Named("toArray")
    default Map<String, Double[][]> toArray(Map<String, List<List<Double>>> value) {
        return value.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    Double[][] result = new Double[entry.getValue().size()][];
                    for (int i = 0; i < entry.getValue().size(); i++) {
                        List<Double> row = entry.getValue().get(i);
                        result[i] = row.toArray(new Double[0]);
                    }
                    return result;
                }
        ));
    }
}
