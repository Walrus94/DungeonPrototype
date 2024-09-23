package org.dungeon.prototype.service.effect;

import lombok.val;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.service.BaseServiceUnitTest;
import org.dungeon.prototype.service.PlayerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.dungeon.prototype.TestData.getMonster;
import static org.dungeon.prototype.TestData.getMonsterEffects;
import static org.dungeon.prototype.TestData.getPlayer;
import static org.dungeon.prototype.TestData.getPlayerEffects;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class EffectServiceTest extends BaseServiceUnitTest {

    @InjectMocks
    private EffectService effectService;
    @Mock
    private PlayerService playerService;

    @Test
    @DisplayName("Successfully updates monster effects")
    void updateMonsterEffects() {
        Monster monster = getMonster(10 ,20);
        monster.setEffects(getMonsterEffects());

        val updatedMonster = effectService.updateMonsterEffects(monster);

        assertEquals(2, updatedMonster.getEffects().size());
        assertEquals(15, updatedMonster.getHp());
        assertTrue(updatedMonster.getEffects().stream()
                .anyMatch(effect -> EffectAttribute.MOVING.equals(effect.getAttribute())));
        assertTrue(updatedMonster.getEffects().stream()
                .anyMatch(effect -> EffectAttribute.HEALTH.equals(effect.getAttribute())));
    }

    @Test
    @DisplayName("Successfully updates player effects")
    void updatePlayerEffects() {
        val player = getPlayer(CHAT_ID);
        player.setEffects(getPlayerEffects());

        when(playerService.getDefaultMaxHp(player)).thenReturn(100);
        when(playerService.getDefaultMaxMana(player)).thenReturn(10);

        val updatedPlayer = effectService.updatePlayerEffects(player);

        assertEquals(3, updatedPlayer.getEffects().size());
        assertEquals(6, updatedPlayer.getMana());
        assertEquals(1.05 * 0.1, updatedPlayer.getChanceToDodge());


    }

    @Test
    @DisplayName("Successfully updates player's armor effect")
    void updateArmorEffect() {
        val player = getPlayer(CHAT_ID);
        player.setEffects(getPlayerEffects());

        val updatedPlayer = effectService.updateArmorEffect(player);

        assertEquals(10, updatedPlayer.getMaxDefense());
    }
}