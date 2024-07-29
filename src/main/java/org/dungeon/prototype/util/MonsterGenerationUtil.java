package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;

import java.util.BitSet;

@UtilityClass
public class MonsterGenerationUtil {
    public static BitSet getDefaultAttackPattern() {
        return BitSet.valueOf(new byte[]{1, 1, 1, 0});
    }
}
