package necesse.entity.levelEvent.settlementRaidEvent;

import java.util.ArrayList;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadout;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadoutGenerator;

public class ChickenPeopleSettlementRaidLevelEvent extends BasicSettlementRaidLevelEvent {
   public ChickenPeopleSettlementRaidLevelEvent() {
      super("chickenraider", "chickenpeopleraidertype");
   }

   @Override
   public void modifyLoadouts(SettlementRaidLoadoutGenerator generator) {
      ArrayList<SettlementRaidLoadout.Weapon> weapons = new ArrayList<>();
      new SettlementRaidLoadout.Weapon("boxingglovegun").overrideObtained().setWeight(0.25F).addTiersToList(weapons, 10, 55);
      new SettlementRaidLoadout.Weapon("snowlauncher").overrideObtained().setWeight(0.1F).addTiersToList(weapons, 0, 0);
      generator.weapons(weapons);
      generator.maxWeaponsInPool(4);
   }
}
