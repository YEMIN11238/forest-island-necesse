package necesse.level.maps.levelData.settlementData.settler.thoughts;

import necesse.engine.localization.message.GameMessage;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.Settler;

public class QualityOfLifeSettlerThought extends SettlerThought {
   @Override
   public boolean shouldShowThought(HumanMob mob) {
      return false;
   }

   @Override
   protected GameMessage getDescription(HumanMob mob) {
      return Settler.getQualityOfLife(mob.getSettlerQualityOfLife()).getDescription();
   }

   @Override
   protected int getHappinessModifier(HumanMob mob) {
      return (HumanMob.HAPPINESS_QOL_DECREASE_THRESHOLD - HumanMob.HAPPINESS_QOL_INCREASE_THRESHOLD) * mob.getSettlerQualityOfLife();
   }
}
