package necesse.level.maps.levelData.settlementData.settler.thoughts;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.friendly.human.HumanMob;

public class RoomQualitySettlerThought extends SettlerThought {
   public GameMessage description;
   public int minScore;
   public int happinessModifier;

   public RoomQualitySettlerThought(String qualityLocalizationKey, int minScore, int happinessModifier) {
      this.description = new LocalMessage("settlement", "roommood").addReplacement("quality", new LocalMessage("settlement", qualityLocalizationKey));
      this.minScore = minScore;
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
