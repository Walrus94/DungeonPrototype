package org.dungeon.prototype.service.room;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.room.RoomContent;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.HealthShrine;
import org.dungeon.prototype.model.room.content.ManaShrine;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.room.content.Monster;
import org.dungeon.prototype.model.room.content.NormalRoom;
import org.dungeon.prototype.model.room.content.Treasure;

import static org.dungeon.prototype.util.RandomUtil.getNextUniform;
import static org.dungeon.prototype.util.RandomUtil.getNormalDistributionRandomDouble;


@Slf4j
@UtilityClass
public class RoomContentRandomFactory {
    //TODO: move to properties
    private static final Double WEIGHT_PLAY_RANGE = 0.2;

    public static RoomContent getNextRoomContent(RoomType roomType, Integer expectedWeightAbs) {
        log.debug("Generating room content with type {}, expected weight: {}", roomType, expectedWeightAbs);
        return switch (roomType) {
            case MONSTER -> expectedWeightAbs == 0 ? new NormalRoom() : getMonster(expectedWeightAbs);
            case TREASURE -> expectedWeightAbs == 0 ? new NormalRoom() : getTreasure(expectedWeightAbs);
            case MERCHANT -> expectedWeightAbs == 0 ? new NormalRoom() : getMerchant(expectedWeightAbs);
            case HEALTH_SHRINE -> new HealthShrine();
            case MANA_SHRINE -> new ManaShrine();
            default -> new NormalRoom();
        };
    }

    private static Merchant getMerchant(Integer expectedWeight) {
        //TODO: implement random items generator
        return new Merchant();
    }

    //TODO: add random items
    private static Treasure getTreasure(Integer expectedWeight) {
        return new Treasure((int) ((getNormalDistributionRandomDouble(expectedWeight.doubleValue(),
                expectedWeight.doubleValue() * 0.1 ) * (1.0 + getNextUniform(-WEIGHT_PLAY_RANGE, WEIGHT_PLAY_RANGE)))));
    }

    //TODO: add Monster factory when diff kinds are available
    private static Monster getMonster(Integer expectedWeight) {
        val weight = getNormalDistributionRandomDouble(expectedWeight.doubleValue(), expectedWeight.doubleValue() * 0.1) * (1.0 + getNextUniform(-WEIGHT_PLAY_RANGE, WEIGHT_PLAY_RANGE));
        val monster = new Monster();
        var level = (int) (weight * 0.01);
        if (level < 1) {
            level = 1;
        }
        monster.setLevel(level);
        monster.setAttack(5 * level);
        monster.setMaxHp(10 * level);
        monster.setHp(monster.getMaxHp());
        monster.setXpReward(monster.getAttack() + monster.getMaxHp() * 10);
        return monster;
    }
}
