package necesse.inventory.item.miscItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

public class VoidPouchItem extends CloudInventoryOpenItem {
   public VoidPouchItem() {
      super(false, 0, 19);
      this.setItemCategory("misc", "pouches");
      this.rarity = Item.Rarity.EPIC;
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "voidpouchtip"));
      tooltips.add(Localization.translate("itemtooltip", "rclickopentip"));
      return tooltips;
   }
}
