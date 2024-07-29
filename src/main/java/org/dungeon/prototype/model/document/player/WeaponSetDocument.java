package org.dungeon.prototype.model.document.player;

import lombok.Data;
import org.dungeon.prototype.model.document.item.ItemDocument;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "weapon_set")
public class WeaponSetDocument {
    @Id
    String id;
    @DBRef
    private ItemDocument primaryWeapon;
    @DBRef
    private ItemDocument secondaryWeapon;
}
