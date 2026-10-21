package necesse.inventory.item.upgradeUtils;

import java.util.TreeMap;
import java.util.Map.Entry;
import necesse.engine.util.GameMath;

public class FloatUpgradeValue extends AbstractUpgradeValue<Float> {
   public boolean inverse;
   public float defaultValue;
   public float defaultLevelIncreaseMultiplier;
   private TreeMap<Float, Float> map = new TreeMap<>();

   public FloatUpgradeValue(boolean inverse, float defaultValue, float defaultLevelIncreaseMultiplier) {
      this.inverse = inverse;
      this.defaultValue = defaultValue;
      this.defaultLevelIncreaseMultiplier = defaultLevelIncreaseMultiplier;
   }

   public FloatUpgradeValue(float defaultValue, float defaultLevelIncreaseMultiplier) {
      this(false, defaultValue, defaultLevelIncreaseMultiplier);
   }

   public FloatUpgradeValue() {
      this(false, 0.0F, 0.0F);
   }

   public FloatUpgradeValue setBaseValue(float value) {
      return this.setUpgradedValue(0.0F, value);
   }

   public FloatUpgradeValue setUpgradedValue(float tier, float value) {
      this.map.put(tier, value);
      return this;
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   public boolean hasMoreThanOneValue() {
      return this.map.size() > 1;
   }

   public Float getValue(float tier) {
      Entry<Float, Float> floorEntry = null;
      Entry<Float, Float> ceilEntry = null;
      Entry<Float, Float> closestEntry = this.map.floorEntry(tier);
      if (closestEntry != null) {
         if (tier == closestEntry.getKey()) {
            return closestEntry.getValue();
         }

         floorEntry = closestEntry;
         Entry<Float, Float> higher = this.map.higherEntry(tier);
         if (higher != null) {
            ceilEntry = higher;
         } else {
            floorEntry = this.map.lowerEntry(closestEntry.getKey());
            ceilEntry = closestEntry;
         }
      } else {
         Entry<Float, Float> higher = this.map.higherEntry(tier);
         if (higher != null) {
            floorEntry = higher;
            ceilEntry = this.map.higherEntry(higher.getKey());
         }
      }

      if (floorEntry != null && ceilEntry != null) {
         if (floorEntry.getKey() < 1.0F) {
            float valueAtTier1;
            if (ceilEntry.getKey() > 1.0F) {
               valueAtTier1 = ceilEntry.getValue() / (1.0F + this.defaultLevelIncreaseMultiplier * ceilEntry.getKey());
            } else if (ceilEntry.getKey() < 1.0F) {
               float tier1UpgradePercentage = GameMath.getPercentageBetweenTwoNumbers(1.0F, floorEntry.getKey(), ceilEntry.getKey());
               valueAtTier1 = GameMath.lerp(tier1UpgradePercentage, floorEntry.getValue(), ceilEntry.getValue());
            } else {
               valueAtTier1 = ceilEntry.getValue();
            }

            if (tier > 1.0F) {
               float modifier = 1.0F + (tier - 1.0F) * this.defaultLevelIncreaseMultiplier;
               return this.inverse ? valueAtTier1 * (1.0F / modifier) : valueAtTier1 * modifier;
            } else {
               float upgradeLevelPercentage = GameMath.getPercentageBetweenTwoNumbers(tier, floorEntry.getKey(), 1.0F);
               return GameMath.lerp(upgradeLevelPercentage, floorEntry.getValue(), valueAtTier1);
            }
         } else {
            float upgradeLevelPercentage = GameMath.getPercentageBetweenTwoNumbers(tier, floorEntry.getKey(), ceilEntry.getKey());
            return GameMath.lerp(upgradeLevelPercentage, floorEntry.getValue(), ceilEntry.getValue());
         }
      } else if (floorEntry != null) {
         return floorEntry.getValue();
      } else {
         return ceilEntry != null ? ceilEntry.getValue() : this.defaultValue;
      }
   }
}
