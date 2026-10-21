package necesse.level.maps.levelData.settlementData.settler.thoughts;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.friendly.human.HumanMob;

public class RoomSizeSettlerThought extends SettlerThought {
   public GameMessage description;
   public int minSize;
   public int happinessModifier;

   public RoomSizeSettlerThought(String sizeLocalizationKey, int minSize, int happinessModifier) {
      this.description = new LocalMessage("settlement", "sizemood").addReplacement("size", new LocalMessage("settlement", sizeLocalizationKey));
      this.minSize = minSize;
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
