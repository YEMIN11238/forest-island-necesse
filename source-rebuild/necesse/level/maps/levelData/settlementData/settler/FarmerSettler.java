package necesse.level.maps.levelData.settlementData.settler;

import java.util.function.Supplier;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class FarmerSettler extends Settler {
   public FarmerSettler() {
      super("farmerhuman");
   }

   @Override
   public GameMessage getAcquireTip() {
      return new LocalMessage("settlement", "foundinvillagetip");
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
      drawOptions.helmet(new InventoryItem("farmerhat"));
      drawOptions.chestplate(new InventoryItem("farmershirt"));
      drawOptions.boots(new InventoryItem("farmershoes"));
      drawOptions.holdItem(new InventoryItem("farmerpitchfork"));
   }

   @Override
   public void addNewRecruitSettler(ServerSettlementData data, boolean isRandomEvent, TicketSystemList<Supplier<HumanMob>> ticketSystem) {
      if (isRandomEvent || !this.doesSettlementHaveThisSettler(data)) {
         ticketSystem.addObject(100, this.getNewRecruitMob(data));
      }
   }
}
