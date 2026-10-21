package necesse.entity.levelEvent.settlementRaidEvent;

import java.util.ArrayList;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadout;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadoutGenerator;

public class TheMafiaRaidLevelEvent extends BasicSettlementRaidLevelEvent {
   public TheMafiaRaidLevelEvent() {
      super("mafiaraider", "themafiaraidertype");
   }

   @Override
   public void modifyLoadouts(SettlementRaidLoadoutGenerator generator) {
      ArrayList<SettlementRaidLoadout.Weapon> weapons = new ArrayList<>();
      new SettlementRaidLoadout.Weapon("handgun").overrideObtained().setWeight(0.4F).addTiersToList(weapons, 12, 72);
      new SettlementRaidLoadout.Weapon("machinegun").overrideObtained().setWeight(0.25F).addTiersToList(weapons, 8, 36);
      new SettlementRaidLoadout.Weapon("shotgun").overrideObtained().setWeight(0.25F).addTiersToList(weapons, 12, 38);
      new SettlementRaidLoadout.Weapon("sniperrifle").overrideObtained().setWeight(0.1F).addTiersToList(weapons, 1050, Integer.MAX_VALUE, 18, 111);
      generator.weapons(weapons);
      generator.maxWeaponsInPool(8);
   }
}
