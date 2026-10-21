package necesse.entity.levelEvent.settlementRaidEvent;

import necesse.level.maps.levelData.settlementData.SettlementRaidLoadoutGenerator;

public class FishianSettlementRaidLevelEvent extends BasicSettlementRaidLevelEvent {
   public FishianSettlementRaidLevelEvent() {
      super("fishianraider", "fishianraidertype");
   }

   @Override
   public void modifyLoadouts(SettlementRaidLoadoutGenerator generator) {
      super.modifyLoadouts(generator);
   }
}
