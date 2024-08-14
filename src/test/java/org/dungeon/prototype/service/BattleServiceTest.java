package org.dungeon.prototype.service;

import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.properties.BattleProperties;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BattleServiceTest {

    @Mock
    private BattleProperties battleProperties;

    @InjectMocks
    private BattleService battleService;

    @Test
    public void monsterAttacks() {
        Player player = getPlayer();
        Monster monster = getMonster();

        battleService.monsterAttacks(player, monster);
    }

    @Test
    public void playerAttacks() {
    }

    private Player getPlayer() {
        Player player = new Player();

        return player;
    }

    private Monster getMonster() {
        Monster monster = new Monster();

        return monster;
    }
}