package necesse.engine.incursionPerkTree;

import necesse.entity.levelEvent.incursionModifiers.AllBannerBuffsLevelEvent;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.incursion.AltarData;

public class AllBannerBuffsPerk extends IncursionPerk {
   public AllBannerBuffsPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }

   @Override
   public void onIncursionLevelGenerated(IncursionLevel level, AltarData altarData, int modifierIndex) {
      AllBannerBuffsLevelEvent event = new AllBannerBuffsLevelEvent();
      level.entityManager.events.add(event);
      level.gndData.setInt(this.getStringID() + modifierIndex, event.getUniqueID());
   }
}
