package org.dungeon.prototype.model.inventory.attributes;

import lombok.Value;
import org.apache.commons.math3.linear.ArrayRealVector;

@Value(staticConstructor = "of")
public class MagicType {
    Double divineMagic;//negative values are Necromantic
    Double arcaneMagic;//negative values are Chaotic

    public ArrayRealVector toVector() {
        return new ArrayRealVector(new double[]{divineMagic, arcaneMagic});
    }

    @Override
    public String toString() {
        if (divineMagic == 0 && arcaneMagic == 0) {
            return "Empty type";
        }

        if (divineMagic == 0) {
            if (arcaneMagic > 0) {
                return "Arcane";
            } else {
                return "Chaotic";
            }
        }

        if (arcaneMagic == 0) {
            if (divineMagic > 0) {
                return "Divine";
            } else {
                return "Necromantic";
            }
        }

        if (divineMagic * arcaneMagic > 0) {
            if (divineMagic > 0) {
                return "Arcane Divine";
            } else {
                return "Chaotic Necromantic";
            }
        } else {
            if (divineMagic > 0) {
                return "Chaotic Divine";
            } else {
                return "Arcane Necromantic";
            }
        }
    }
}
