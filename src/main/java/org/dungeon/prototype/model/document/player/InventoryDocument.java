package org.dungeon.prototype.model.document.player;

import lombok.Data;
import org.dungeon.prototype.model.document.item.ItemDocument;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "inventory")
public class InventoryDocument {
    @Id
    private String id;
    @DBRef
    private List<ItemDocument> items;
    @DBRef
    private ItemDocument helmet;
    @DBRef
    private ItemDocument vest;
    @DBRef
    private ItemDocument gloves;
    @DBRef
    private ItemDocument boots;
    @DBRef
    private ItemDocument primaryWeapon;
    @DBRef
    private ItemDocument secondaryWeapon;
}
