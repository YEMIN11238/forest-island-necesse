package necesse.entity.levelEvent.settlementRaidEvent;

import java.util.ArrayList;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadout;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadoutGenerator;

public class MummiesSettlementRaidLevelEvent extends BasicSettlementRaidLevelEvent {
   public MummiesSettlementRaidLevelEvent() {
      super("mummyraider", "mummyraidertype");
   }

   @Override
   public void modifyLoadouts(SettlementRaidLoadoutGenerator generator) {
      ArrayList<SettlementRaidLoadout.Weapon> weapons = new ArrayList<>();
      new SettlementRaidLoadout.Weapon("quartzgreatsword").setWeight(0.1F).addTiersToList(weapons, 100, Integer.MAX_VALUE, 38, 112);
      new SettlementRaidLoadout.Weapon("quartzglaive").setWeight(0.1F).addTiersToList(weapons, 100, Integer.MAX_VALUE, 16, 62);
      new SettlementRaidLoadout.Weapon("quartzstaff").setWeight(0.1F).addTiersToList(weapons, 100, Integer.MAX_VALUE, 15, 92);
      new SettlementRaidLoadout.Weapon("amethyststaff").setWeight(0.1F).addTiersToList(weapons, 1050, Integer.MAX_VALUE, 12, 41);
      new SettlementRaidLoadout.Weapon("amethystsword").setWeight(0.3F).addTiersToList(weapons, 100, Integer.MAX_VALUE, 16, 72);
      new SettlementRaidLoadout.Weapon("myceliumgreatbow").setWeight(0.15F).addTiersToList(weapons, 1500, Integer.MAX_VALUE, 16, 98);
      new SettlementRaidLoadout.Weapon("emeraldstaff").setWeight(0.15F).addTiersToList(weapons, 1500, Integer.MAX_VALUE, 10, 26);
      new SettlementRaidLoadout.Weapon("ancientdredgingstaff").setWeight(0.1F).overrideObtained().addTiersToList(weapons, 1750, Integer.MAX_VALUE, 13, 66);
      generator.weapons(weapons);
      generator.maxWeaponsInPool(12);
   }
}
