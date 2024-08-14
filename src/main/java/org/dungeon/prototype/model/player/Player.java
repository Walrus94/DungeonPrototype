package org.dungeon.prototype.model.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.effect.PlayerEffect;
import org.dungeon.prototype.model.effect.attributes.PlayerEffectAttribute;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.service.PlayerLevelService;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Data
@Slf4j
@NoArgsConstructor
public class Player {
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
    private Inventory inventory;
    private Map<PlayerEffectAttribute, PriorityQueue<PlayerEffect>> effects;
    EnumMap<PlayerAttribute, Integer> attributes;

    //TODO: move to service and use queries through repository
    public boolean addXp(Integer xpReward) {
        xp += xpReward;
        log.debug("Rewarded xp: {}, total: {}", xpReward, xp);
        val level = PlayerLevelService.getLevel(xp);
        if (level == playerLevel + 1) {
            playerLevel++;
            log.debug("Level {} achieved!", playerLevel);
            refillHp();
            refillMana();
            nextLevelXp = PlayerLevelService.calculateXPForLevel(playerLevel + 1);
            return true;
        }
        return false;
    }
    public void decreaseDefence(int amount) {
        defense -= amount;
    }
    public void decreaseHp(int amount) {
        hp -= amount;
    }
    public void addGold(int amount) {
        gold += amount;
    }
    public void removeGold(int amount) {
        gold = amount > gold ? 0 : gold - amount;
    }
    public void refillHp() {
        hp = maxHp;
    }
    public void refillMana() {
        mana = maxMana;
    }
    public void restoreArmor() {
        defense = maxDefense;
    }

    public void addEffects(List<PlayerEffect> effects) {
        effects.forEach(playerEffect ->
                this.effects.get(playerEffect.getAttribute()).offer(playerEffect));
    }
}
