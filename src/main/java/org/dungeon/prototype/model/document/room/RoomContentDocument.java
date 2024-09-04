package org.dungeon.prototype.model.document.room;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.item.EffectDocument;
import org.dungeon.prototype.model.document.item.ItemDocument;
import org.dungeon.prototype.model.document.monster.MonsterDocument;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.annotations.validation.MultiConditionalNotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@MultiConditionalNotNull(conditions = {
        @MultiConditionalNotNull.Condition(
                field = "monster",
                conditionalField = "roomType",
                conditionalValues = {"WEREWOLF", "VAMPIRE", "SWAMP_BEAST", "DRAGON", "ZOMBIE"}
        ),
        @MultiConditionalNotNull.Condition(
                field = "gold",
                conditionalField = "roomType",
                conditionalValues = {"TREASURE"}
        ),
        @MultiConditionalNotNull.Condition(
                field = "items",
                conditionalField = "roomType",
                conditionalValues = {"TREASURE", "MERCHANT"}
        )
})
@Document(collection = "roomContent")
public class RoomContentDocument {
    @Id
    String id;
    @DBRef
    private MonsterDocument monster;
    private Integer gold;
    @DBRef
    private List<ItemDocument> items;

    private EffectDocument effect;

    private Double chanceToBreakWeapon;
    private Integer attackBonus;
    private boolean armorRestored;

    private Integer roomContentWeight;
    private RoomType roomType;
}
