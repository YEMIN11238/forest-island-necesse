package necesse.inventory.item.placeableItem.consumableItem.food;

import necesse.engine.modifiers.ModifierValue;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.ChickenMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.levelData.settlementData.settler.FoodQuality;

public class EggFoodConsumableItem extends FoodConsumableItem implements EggItemInterface {
   public EggFoodConsumableItem(
      int stackSize, Item.Rarity rarity, FoodQuality quality, int nutrition, int buffSecondsDuration, boolean drinkSound, ModifierValue<?>... modifiers
   ) {
      super(stackSize, rarity, quality, nutrition, buffSecondsDuration, drinkSound, modifiers);
      this.dropDecayTimeMillis = 600000L;
   }

   public EggFoodConsumableItem(int stackSize, Item.Rarity rarity, FoodQuality quality, int nutrition, int buffSecondsDuration, ModifierValue<?>... modifiers) {
      super(stackSize, rarity, quality, nutrition, buffSecondsDuration, modifiers);
      this.dropDecayTimeMillis = 600000L;
   }

   @Override
   public String getHatchMobStringID(InventoryItem item) {
      return GameRandom.globalRandom.getEveryXthChance(4) ? "rooster" : "chicken";
   }

   @Override
   public int getRandomHatchTimeSeconds(InventoryItem item) {
      return GameRandom.globalRandom.getIntBetween(ChickenMob.EGG_HATCH_SECONDS_MIN, ChickenMob.EGG_HATCH_SECONDS_MAX);
   }
}
