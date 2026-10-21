package necesse.engine.expeditions;

import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public abstract class FishingTripExpedition extends SettlerExpedition {
   @Override
   public float getSuccessChance(ServerSettlementData settlement) {
      return 1.0F;
   }
}
