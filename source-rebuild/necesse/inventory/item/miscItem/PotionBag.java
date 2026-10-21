package necesse.inventory.item.miscItem;

import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;

public class PotionBag extends PotionPouch {
   public PotionBag() {
      this.rarity = Item.Rarity.RARE;
      this.setItemCategory(ItemCategory.craftingManager, "equipment");
   }

   @Override
   public int getInternalInventorySize() {
      return 20;
   }
}
