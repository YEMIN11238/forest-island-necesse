package necesse.engine.incursionPerkTree;

import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.IncursionPerkModifiers.MobsDropUpgradeShardsPerkLevelEvent;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.incursion.AltarData;

public class MobsDropUpgradeShardsPerk extends IncursionPerk {
   public MobsDropUpgradeShardsPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }

   @Override
   public void onIncursionLevelGenerated(IncursionLevel level, AltarData altarData, int modifierIndex) {
      MobsDropUpgradeShardsPerkLevelEvent event = new MobsDropUpgradeShardsPerkLevelEvent();
      level.entityManager.events.add(event);
      level.gndData.setInt(this.getStringID() + modifierIndex, event.getUniqueID());
   }

   @Override
   public void onIncursionLevelCompleted(IncursionLevel level, AltarData altarData, int modifierIndex) {
      int eventUniqueID = level.gndData.getInt(this.getStringID() + modifierIndex);
      LevelEvent event = level.entityManager.events.get(eventUniqueID, false);
      if (event != null) {
         event.over();
      }
   }
}
