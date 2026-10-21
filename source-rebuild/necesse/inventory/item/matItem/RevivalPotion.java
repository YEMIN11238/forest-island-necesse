package necesse.inventory.item.matItem;

import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;

public class RevivalPotion extends MatItem {
   public RevivalPotion() {
      super(250, Item.Rarity.RARE, "revivalpot");
      this.setItemCategory(ItemCategory.craftingManager, "consumable", "resourcepotions");
   }
}
