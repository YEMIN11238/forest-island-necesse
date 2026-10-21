package necesse.inventory.item.upgradeUtils;

import java.util.TreeMap;
import java.util.Map.Entry;
import necesse.engine.util.GameMath;

public class IntUpgradeValue extends AbstractUpgradeValue<Integer> {
   public boolean inverse;
   public int defaultValue;
   public float defaultLevelIncreaseMultiplier;
   private TreeMap<Float, Integer> map = new TreeMap<>();

   public IntUpgradeValue(boolean inverse, int defaultValue, float defaultLevelIncreaseMultiplier) {
      this.inverse = inverse;
      this.defaultValue = defaultValue;
      this.defaultLevelIncreaseMultiplier = defaultLevelIncreaseMultiplier;
   }

   public IntUpgradeValue(int defaultValue, float defaultLevelIncreaseMultiplier) {
      this(false, defaultValue, defaultLevelIncreaseMultiplier);
   }

   public IntUpgradeValue() {
      this(false, 0, 0.0F);
   }

   public IntUpgradeValue setBaseValue(int value) {
      return this.setUpgradedValue(0.0F, value);
   }

   public IntUpgradeValue setUpgradedValue(float tier, int value) {
      this.map.put(tier, value);
      return this;
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   public boolean hasMoreThanOneValue() {
      return this.map.size() > 1;
   }

   public Integer getValue(float tier) {
      Entry<Float, Integer> floorEntry = null;
      Entry<Float, Integer> ceilEntry = null;
      Entry<Float, Integer> closestEntry = this.map.floorEntry(tier);
      if (closestEntry != null) {
         if (tier == closestEntry.getKey()) {
            return closestEntry.getValue();
         }

         floorEntry = closestEntry;
         Entry<Float, Integer> higher = this.map.higherEntry(tier);
         if (higher != null) {
            ceilEntry = higher;
         } else {
            floorEntry = this.map.lowerEntry(closestEntry.getKey());
            ceilEntry = closestEntry;
         }
      } else {
         Entry<Float, Integer> higher = this.map.higherEntry(tier);
         if (higher != null) {
            floorEntry = higher;
            ceilEntry = this.map.higherEntry(higher.getKey());
         }
      }

      if (floorEntry != null && ceilEntry != null) {
         if (floorEntry.getKey() < 1.0F) {
            int valueAtTier1;
            if (ceilEntry.getKey() > 1.0F) {
               valueAtTier1 = Math.round(ceilEntry.getValue().intValue() / (1.0F + this.defaultLevelIncreaseMultiplier * ceilEntry.getKey()));
            } else if (ceilEntry.getKey() < 1.0F) {
               float tier1UpgradePercentage = GameMath.getPercentageBetweenTwoNumbers(1.0F, floorEntry.getKey(), ceilEntry.getKey());
               valueAtTier1 = GameMath.lerp(tier1UpgradePercentage, floorEntry.getValue(), ceilEntry.getValue());
            } else {
               valueAtTier1 = ceilEntry.getValue();
            }

            if (tier > 1.0F) {
               float modifier = 1.0F + (tier - 1.0F) * this.defaultLevelIncreaseMultiplier;
               return this.inverse ? Math.round(valueAtTier1 * (1.0F / modifier)) : Math.round(valueAtTier1 * modifier);
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
