package necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.utilityBuffPotions;

import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.SimplePotionItem;

public class TrackerPotionItem extends SimplePotionItem {
   public TrackerPotionItem() {
      super(100, Item.Rarity.RARE, "trackerpotion", 600, "trackerpot");
      this.setItemCategory(ItemCategory.craftingManager, "consumable", "utilitypotions");
   }
}
