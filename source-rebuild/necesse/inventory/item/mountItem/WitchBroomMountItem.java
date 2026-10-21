package necesse.inventory.item.mountItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

public class WitchBroomMountItem extends MountItem {
   public WitchBroomMountItem() {
      super("witchbroom");
      this.rarity = Item.Rarity.RARE;
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "witchbroomtip1"));
      tooltips.add(Localization.translate("itemtooltip", "witchbroomtip2"));
      tooltips.add(Localization.translate("itemtooltip", "witchbroomtip3"));
      tooltips.add(Localization.translate("itemtooltip", "staminausertip"));
      return tooltips;
   }
}
