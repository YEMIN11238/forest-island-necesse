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

public class GunsmithSettler extends Settler {
   public GunsmithSettler() {
      super("gunsmithhuman");
   }

   @Override
   public GameMessage getAcquireTip() {
      return new LocalMessage("settlement", "foundinvillagetip");
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
      drawOptions.helmet(new InventoryItem("engineergoggles"));
      drawOptions.chestplate(new InventoryItem("labapron"));
      drawOptions.boots(new InventoryItem("labboots"));
   }

   @Override
   public void addNewRecruitSettler(ServerSettlementData data, boolean isRandomEvent, TicketSystemList<Supplier<HumanMob>> ticketSystem) {
      if ((isRandomEvent || !this.doesSettlementHaveThisSettler(data)) && data.storyObjectives.hasCompletedStoryObjectiveOrHigher("defeatevilsprotector")) {
         ticketSystem.addObject(75, this.getNewRecruitMob(data));
      }
   }
}
