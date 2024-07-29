package org.dungeon.prototype.model.document.item;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "items")
@NoArgsConstructor
public class ItemDocument {
    @Id
    private String id;
    @Indexed
    private Long chatId;
    private ItemAttributes attributes;
    private ItemType itemType;
    private ItemSpecs specs;
    private String name;
    @Indexed
    private Integer weight;
    private boolean hasMagic;
    private MagicType magicType;
//    @DBRef
//    private List<EffectDocument> effects;

    private Integer sellingPrice;
    private Integer buyingPrice;
}
