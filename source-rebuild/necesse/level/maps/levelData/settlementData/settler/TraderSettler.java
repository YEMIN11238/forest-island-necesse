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

public class TraderSettler extends Settler {
   public TraderSettler() {
      super("traderhuman");
   }

   @Override
   public GameMessage getAcquireTip() {
      return new LocalMessage("settlement", "tradertip");
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
      drawOptions.helmet(null);
      drawOptions.chestplate(new InventoryItem("merchantshirt"));
      drawOptions.boots(new InventoryItem("merchantboots"));
   }

   @Override
   public void addNewRecruitSettler(ServerSettlementData data, boolean isRandomEvent, TicketSystemList<Supplier<HumanMob>> ticketSystem) {
      if ((isRandomEvent || !this.doesSettlementHaveThisSettler(data)) && data.storyObjectives.hasCompletedStoryObjectiveOrHigher("defeatchieftain")) {
         ticketSystem.addObject(100, this.getNewRecruitMob(data));
      }
   }
}
