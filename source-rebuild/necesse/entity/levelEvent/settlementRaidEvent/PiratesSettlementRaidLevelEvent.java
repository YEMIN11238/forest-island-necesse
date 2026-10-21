package necesse.entity.levelEvent.settlementRaidEvent;

import java.util.ArrayList;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadout;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadoutGenerator;

public class PiratesSettlementRaidLevelEvent extends BasicSettlementRaidLevelEvent {
   public PiratesSettlementRaidLevelEvent() {
      super("pirateraider", "pirateraidertype");
   }

   @Override
   public void modifyLoadouts(SettlementRaidLoadoutGenerator generator) {
      ArrayList<SettlementRaidLoadout.Weapon> weapons = new ArrayList<>();
      new SettlementRaidLoadout.Weapon("goldsword").setWeight(0.35F).addTiersToList(weapons, 100, 1200, 16, 82);
      new SettlementRaidLoadout.Weapon("goldglaive").setWeight(0.3F).addTiersToList(weapons, 100, 1200, 14, 64);
      new SettlementRaidLoadout.Weapon("goldgreatbow").setWeight(0.2F).addTiersToList(weapons, 100, 1200, 26, 98);
      new SettlementRaidLoadout.Weapon("goldbow").setWeight(0.15F).addTiersToList(weapons, 100, 1200, 13, 76);
      new SettlementRaidLoadout.Weapon("cutlass").overrideObtained().setWeight(0.4F).addTiersToList(weapons, 1200, Integer.MAX_VALUE, 15, 78);
      new SettlementRaidLoadout.Weapon("flintlock").overrideObtained().setWeight(0.3F).addTiersToList(weapons, 1200, Integer.MAX_VALUE, 13, 72);
      new SettlementRaidLoadout.Weapon("handcannon").setWeight(0.1F).addTiersToList(weapons, 1200, Integer.MAX_VALUE, 13, 140);
      new SettlementRaidLoadout.Weapon("genielamp").overrideObtained().setWeight(0.2F).addTiersToList(weapons, 1200, Integer.MAX_VALUE, 12, 36);
      generator.weapons(weapons);
      generator.maxWeaponsInPool(5);
   }
}
