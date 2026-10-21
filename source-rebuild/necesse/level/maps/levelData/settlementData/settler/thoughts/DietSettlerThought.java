package necesse.level.maps.levelData.settlementData.settler.thoughts;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.friendly.human.HumanMob;

public class DietSettlerThought extends SettlerThought {
   public GameMessage description;
   public final int variety;
   public int happinessModifier;

   public DietSettlerThought(String localizationKey, int variety, int happinessModifier) {
      this.description = new LocalMessage("settlement", localizationKey);
      this.variety = variety;
      this.happinessModifier = happinessModifier;
   }

   @Override
   protected GameMessage getDescription(HumanMob mob) {
      return this.description;
   }

   @Override
   protected int getHappinessModifier(HumanMob mob) {
      return this.happinessModifier;
   }
}
