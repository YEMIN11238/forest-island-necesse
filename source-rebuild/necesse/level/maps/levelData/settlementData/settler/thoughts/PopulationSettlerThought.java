package necesse.level.maps.levelData.settlementData.settler.thoughts;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.friendly.human.HumanMob;

public class PopulationSettlerThought extends SettlerThought {
   public GameMessage description;
   public int population;
   public int happinessModifier;

   public PopulationSettlerThought(String localizationKey, int population, int happinessModifier) {
      this.description = new LocalMessage("settlement", localizationKey);
      this.population = population;
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
