package necesse.entity.mobs;

import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;
import necesse.level.maps.levelData.settlementData.notifications.SettlementNotificationSeverity;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public interface HungerMob {
   float getHungerLevel();

   void useHunger(float var1, boolean var2);

   void addHunger(float var1);

   default boolean useFoodItem(FoodConsumableItem item, boolean giveBuff) {
      if (giveBuff) {
         item.giveFoodBuff((Mob)this);
      }

      this.addHunger(item.nutrition / 100.0F);
      return true;
   }

   default void submitHungryNotification() {
      if (this instanceof SettlerMob && this.isNoFoodNotificationStillValid()) {
         SettlerMob settlerMob = (SettlerMob)this;
         SettlementNotificationSeverity severity = SettlementNotificationSeverity.WARNING;
         if (this.getHungerLevel() <= 0.0F) {
            severity = SettlementNotificationSeverity.URGENT;
         }

         NetworkSettlementData settlement = settlerMob.getSettlerSettlementNetworkData();
         if (settlement != null) {
            settlement.notifications.submitNotification("hungry", (SettlerMob)this, severity);
         }
      }
   }

   default void removeHungryNotification() {
      if (this instanceof SettlerMob) {
         SettlerMob settlerMob = (SettlerMob)this;
         NetworkSettlementData settlement = settlerMob.getSettlerSettlementNetworkData();
         if (settlement != null) {
            settlement.notifications.removeNotification("hungry", (SettlerMob)this);
         }
      }
   }

   default boolean isNoFoodNotificationStillValid() {
      return this.getHungerLevel() <= 0.15F;
   }
}
