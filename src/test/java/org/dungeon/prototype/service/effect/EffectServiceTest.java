package org.dungeon.prototype.service.effect;

import lombok.val;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.service.BaseServiceUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.dungeon.prototype.TestData.getMonsterEffects;
import static org.junit.jupiter.api.Assertions.*;

class EffectServiceTest extends BaseServiceUnitTest {

    @InjectMocks
    private EffectService effectService;

    @Test
    void updateMonsterEffects() {
        Monster monster = new Monster();
        monster.setEffects(getMonsterEffects());

        val updatedMonster = effectService.updateMonsterEffects(monster);

    }

    @Test
    void updatePlayerEffects() {
    }

    @Test
    void updateArmorEffect() {
    }
}