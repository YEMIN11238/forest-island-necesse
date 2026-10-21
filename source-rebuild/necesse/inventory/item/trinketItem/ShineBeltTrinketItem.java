package necesse.inventory.item.trinketItem;

import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.TrinketsLootTable;
import necesse.level.maps.Level;

public class ShineBeltTrinketItem extends SimpleTrinketItem {
   public ShineBeltTrinketItem() {
      super(Item.Rarity.COMMON, "shinebelttrinket", 200, TrinketsLootTable.trinkets);
   }

   @Override
   public void refreshLight(Level level, float x, float y, InventoryItem item, boolean isHolding) {
      level.lightManager.refreshParticleLightFloat(x, y, 50.0F, 0.4F, 135);
   }
}
