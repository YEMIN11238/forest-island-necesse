package necesse.level.maps.levelData.settlementData.settler.thoughts;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.friendly.human.HumanMob;

public class SimpleSettlerThought extends SettlerThought {
   public GameMessage description;
   public int happinessModifier;
   public int maxStacks;

   public SimpleSettlerThought(GameMessage description, int happinessModifier, int maxStacks) {
      this.description = description;
      this.happinessModifier = happinessModifier;
      this.maxStacks = maxStacks;
   }

   public SimpleSettlerThought(GameMessage description, int happinessModifier) {
      this(description, happinessModifier, -1);
   }

   public SimpleSettlerThought(String category, String key, int happinessModifier, int maxStacks) {
      this(new LocalMessage(category, key), happinessModifier, maxStacks);
   }

   public SimpleSettlerThought(String category, String key, int happinessModifier) {
      this(category, key, happinessModifier, -1);
   }

   @Override
   protected GameMessage getDescription(HumanMob mob) {
      return this.description;
   }

   @Override
   protected int getHappinessModifier(HumanMob mob) {
      return this.happinessModifier;
   }

   @Override
   public int getMaxStacks(HumanMob mob) {
      return this.maxStacks <= 0 ? Integer.MAX_VALUE : this.maxStacks;
   }
}
