package necesse.level.maps.levelData.settlementData.settler.thoughts;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.HappinessObject;

public class HappinessObjectSettlerThought extends SettlerThought {
   public final HappinessObject happinessObject;

   public HappinessObjectSettlerThought(HappinessObject happinessObject) {
      this.happinessObject = happinessObject;
   }

   @Override
   protected GameMessage getDescription(HumanMob mob) {
      return new LocalMessage("settlement", "happinessobject", "object", ((GameObject)this.happinessObject).getLocalization());
   }

   @Override
   protected int getHappinessModifier(HumanMob mob) {
      return this.happinessObject.getHappinessBonus();
   }
}
