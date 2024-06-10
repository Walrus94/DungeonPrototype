package org.dungeon.prototype.service;

import lombok.experimental.UtilityClass;

import static org.apache.commons.math3.util.FastMath.floor;
import static org.apache.commons.math3.util.FastMath.pow;

@UtilityClass
public class PlayerLevelService {
    private static final int BASE_XP = 1000; // Base XP required for level 1
    private static final double COEFFICIENT = 50; // Coefficient for polynomial growth
    private static final double POWER = 2; // Power of the polynomial

    public static Integer getLevel(Long xp) {
        if (xp < BASE_XP) {
            return 1; // XP less than BASE_XP corresponds to level 1
        }
        return (int) floor(pow(1 / POWER, (xp - BASE_XP) / COEFFICIENT));
    }

    public static Long calculateXPForLevel(int level) {
        if (level == 1) return 0L;
        return (long) (BASE_XP + COEFFICIENT * pow(POWER, level));
    }
}
