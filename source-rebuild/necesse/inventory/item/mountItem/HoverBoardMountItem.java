package necesse.inventory.item.mountItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

public class HoverBoardMountItem extends MountItem {
   public HoverBoardMountItem() {
      super("hoverboard");
      this.rarity = Item.Rarity.RARE;
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "hoverboardtip"));
      tooltips.add(Localization.translate("itemtooltip", "hoverboardtip2"));
      return tooltips;
   }
}
