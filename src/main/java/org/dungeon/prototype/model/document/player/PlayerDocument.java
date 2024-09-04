package org.dungeon.prototype.model.document.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.document.item.EffectDocument;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.EnumMap;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "players")
public class PlayerDocument {
    @Id
    private String id;
    private Long chatId;
    private String nickname;
    private Point currentRoom;
    private String currentRoomId;
    private Direction direction;
    private Integer gold;
    private Long xp;
    private Integer playerLevel;
    private Long nextLevelXp;
    private Integer hp;
    private Integer maxHp;
    private Integer mana;
    private Integer maxMana;
    private Integer defense;
    private Integer maxDefense;
    private Double chanceToDodge;
    private PlayerAttackDocument primaryAttack;
    private PlayerAttackDocument secondaryAttack;
    private Double xpBonus;
    private Double goldBonus;
    @DBRef
    private InventoryDocument inventory;
    private List<EffectDocument> effects;
    private EnumMap<PlayerAttribute, Integer> attributes;
}
