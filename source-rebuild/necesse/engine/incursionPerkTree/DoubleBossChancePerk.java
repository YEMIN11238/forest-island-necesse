package necesse.engine.incursionPerkTree;

import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.incursionModifiers.DoubleBossChanceLevelEvent;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.incursion.AltarData;
import necesse.level.maps.incursion.SettlementRuinsIncursionLevel;

public class DoubleBossChancePerk extends IncursionPerk {
   public static float CHANCE_TO_INFECT_INCURSION = 0.25F;

   public DoubleBossChancePerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }

   @Override
   public void onIncursionLevelGenerated(IncursionLevel level, AltarData altarData, int modifierIndex) {
      if (!(level instanceof SettlementRuinsIncursionLevel)) {
         if (GameRandom.globalRandom.getChance(CHANCE_TO_INFECT_INCURSION)) {
            DoubleBossChanceLevelEvent event = new DoubleBossChanceLevelEvent();
            level.entityManager.events.add(event);
            level.gndData.setInt(this.getStringID() + modifierIndex, event.getUniqueID());
         }
      }
   }
}
