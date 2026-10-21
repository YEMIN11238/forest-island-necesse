package necesse.level.maps.levelData.settlementData.settler.thoughts;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.personalities.InspiringSettlerPersonality;

public class InspiringHappinessPersonalityThought extends SettlerThought {
   @Override
   protected GameMessage getDescription(HumanMob mob) {
      return new LocalMessage("personalities", "inspiringreceived");
   }

   @Override
   protected int getHappinessModifier(HumanMob mob) {
      return InspiringSettlerPersonality.getCurrentHappinessBonus(0);
   }

   @Override
   public int getStackedHappinessModifier(HumanMob mob, int stacks) {
      return InspiringSettlerPersonality.getCurrentHappinessBonus(stacks);
   }

   @Override
   public boolean shouldClearIfDoesntHavePersonalitySubmitter() {
      return false;
   }
}
