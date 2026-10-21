package necesse.level.maps.levelData.settlementData.settler;

import java.util.function.Supplier;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class AnimalKeeperSettler extends Settler {
   public AnimalKeeperSettler() {
      super("animalkeeperhuman");
   }

   @Override
   public GameMessage getAcquireTip() {
      return new GameMessageBuilder()
         .append(new LocalMessage("settlement", "foundinvillagetip"))
         .append("\n")
         .append(new LocalMessage("settlement", "animalkeepertip"));
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
      drawOptions.helmet(new InventoryItem("animalkeepershat"));
      drawOptions.chestplate(new InventoryItem("animalkeepershirt"));
      drawOptions.boots(new InventoryItem("animalkeepershoes"));
   }

   @Override
   public void addNewRecruitSettler(ServerSettlementData data, boolean isRandomEvent, TicketSystemList<Supplier<HumanMob>> ticketSystem) {
      if ((isRandomEvent || !this.doesSettlementHaveThisSettler(data)) && data.storyObjectives.hasCompletedStoryObjectiveOrHigher("defeatvoidwizard")) {
         ticketSystem.addObject(75, this.getNewRecruitMob(data));
      }
   }
}
