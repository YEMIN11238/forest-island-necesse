package necesse.level.maps.levelData.settlementData.settler.thoughts;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.friendly.human.HumanMob;

public class SharingRoomSettlerThought extends SettlerThought {
   @Override
   protected GameMessage getDescription(HumanMob mob) {
      return new LocalMessage("settlement", "sharingroom");
   }

   @Override
   public int getMaxStacks(HumanMob mob) {
      return 5;
   }

   @Override
   protected int getHappinessModifier(HumanMob mob) {
      return -20;
   }
}
