package necesse.inventory.item.mountItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;

public class SteelBoatMountItem extends MountItem {
   public SteelBoatMountItem() {
      super("steelboat");
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = this.getBaseTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "steelboattip2"));
      tooltips.add(Localization.translate("itemtooltip", "boattip1"));
      return tooltips;
   }
}
