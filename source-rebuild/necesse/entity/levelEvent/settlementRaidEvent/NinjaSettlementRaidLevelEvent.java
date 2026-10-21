package necesse.entity.levelEvent.settlementRaidEvent;

import java.util.ArrayList;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadout;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadoutGenerator;

public class NinjaSettlementRaidLevelEvent extends BasicSettlementRaidLevelEvent {
   public NinjaSettlementRaidLevelEvent() {
      super("ninjaraider", "ninjasraidertype");
   }

   @Override
   public void modifyLoadouts(SettlementRaidLoadoutGenerator generator) {
      ArrayList<SettlementRaidLoadout.Weapon> weapons = new ArrayList<>();
      new SettlementRaidLoadout.Weapon(new InventoryItem("ninjastar", 50)).setWeight(0.25F).addTiersToList(weapons, 100, 1300, 10, 74);
      new SettlementRaidLoadout.Weapon("nunchucks").setWeight(0.25F).addTiersToList(weapons, 100, Integer.MAX_VALUE, 14, 78);
      new SettlementRaidLoadout.Weapon("katana").setWeight(0.15F).addTiersToList(weapons, 100, 1300, 16, 86);
      new SettlementRaidLoadout.Weapon("reinforcedkatana").setWeight(0.15F).addTiersToList(weapons, 1300, Integer.MAX_VALUE, 15, 82);
      new SettlementRaidLoadout.Weapon("tungstenboomerang").setWeight(0.15F).addTiersToList(weapons, 1300, Integer.MAX_VALUE, 13, 68);
      new SettlementRaidLoadout.Weapon("nightrazorboomerang").setWeight(0.1F).addTiersToList(weapons, 2000, Integer.MAX_VALUE, 13, 62);
      generator.weapons(weapons);
      generator.maxWeaponsInPool(4);
   }
}
