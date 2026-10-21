package necesse.entity.mobs;

import necesse.entity.levelEvent.fishingEvent.FishingEvent;
import necesse.entity.pickup.PickupEntity;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.baitItem.BaitItem;
import necesse.inventory.item.placeableItem.fishingRodItem.FishingRodItem;
import necesse.level.maps.biomes.FishingLootTable;
import necesse.level.maps.biomes.FishingSpot;

public interface FishingMob {
   void giveBaitBack(BaitItem var1);

   void stopFishing();

   void showFishingWaitAnimation(FishingRodItem var1, int var2, int var3);

   boolean isFishingSwingDone();

   default FishingLootTable getFishingLootTable(FishingSpot spot) {
      return spot.getBiome().getFishingLootTable(spot);
   }

   default void giveCaughtItem(FishingEvent event, InventoryItem item) {
      PickupEntity pickupItem;
      event.level.entityManager.pickups.add(pickupItem = item.getPickupEntity(event.level, event.getMob().getX(), event.getMob().getY()));
      pickupItem.pickupCooldown = 0;
   }
}
