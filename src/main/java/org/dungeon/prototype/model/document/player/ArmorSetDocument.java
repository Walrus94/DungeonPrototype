package org.dungeon.prototype.model.document.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.item.ItemDocument;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "armor_set")
public class ArmorSetDocument {
    @Id
    private String id;
    @DBRef
    private ItemDocument helmet;
    @DBRef
    private ItemDocument vest;
    @DBRef
    private ItemDocument gloves;
    @DBRef
    private ItemDocument boots;
}
