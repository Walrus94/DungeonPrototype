package org.dungeon.prototype.repository.mongo.converters.mapstruct;

import org.dungeon.prototype.model.document.weight.WeightDocument;
import org.dungeon.prototype.model.weight.Weight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface WeightMapper {
    WeightMapper INSTANCE = Mappers.getMapper(WeightMapper.class);

    @Mapping(target = "id", ignore = true)
    WeightDocument mapToDocument(Weight weight);

    Weight mapToWeight(WeightDocument document);
}
