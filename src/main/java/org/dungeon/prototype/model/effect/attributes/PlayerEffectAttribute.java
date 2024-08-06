package org.dungeon.prototype.model.effect.attributes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PlayerEffectAttribute implements EffectAttribute {
     HEALTH("Health"),
     MANA("Mana"),
     ATTACK("Attack"),
     ARMOR("Armor"),
     CRITICAL_HIT_CHANCE("Critical hit chance"),
     MISS_CHANCE("Miss chance"),
     ADDITIONAL_FIRST_HIT("Additional first hit"),
     KNOCK_OUT_CHANCE("Knock out chance"),
     CHANCE_TO_DODGE("Chance to dodge");

     private final String value;

     PlayerEffectAttribute(String value) {
          this.value = value;
     }

     @Override
     @JsonValue
     public String getValue() {
          return value;
     }

     @JsonCreator
     public static PlayerEffectAttribute fromValue(String value) {
          for (PlayerEffectAttribute e : PlayerEffectAttribute.values()) {
               if (e.value.equalsIgnoreCase(value)) {
                    return e;
               }
          }
          throw new IllegalArgumentException("Unknown player effect attribute type: " + value);
     }

     @Override
     public String toString() {
          return this.value;
     }
}
