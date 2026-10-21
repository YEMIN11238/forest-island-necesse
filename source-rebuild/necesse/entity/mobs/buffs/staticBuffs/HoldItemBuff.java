package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemInventoryItem;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.HumanDrawBuff;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;

public class HoldItemBuff extends Buff implements HumanDrawBuff {
   public HoldItemBuff() {
      this.isVisible = false;
      this.canCancel = false;
      this.shouldSave = false;
   }

   @Override
   public boolean shouldNetworkSync() {
      return false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public boolean shouldDrawDuration(ActiveBuff buff) {
      return false;
   }

   @Override
   public void addHumanDraw(ActiveBuff buff, HumanDrawOptions drawOptions) {
      if (!drawOptions.hasHoldItem()) {
         GNDItem gndItem = buff.getGndData().getItem("holdItem");
         if (gndItem instanceof GNDItemInventoryItem) {
            InventoryItem holdItem = ((GNDItemInventoryItem)gndItem).invItem;
            if (holdItem.item != null) {
               drawOptions.holdItem(holdItem);
            }
         }
      }
   }
}
