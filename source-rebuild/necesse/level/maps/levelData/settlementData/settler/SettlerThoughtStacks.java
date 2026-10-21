package necesse.level.maps.levelData.settlementData.settler;

import necesse.engine.localization.message.GameMessage;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SettlerThought;

public class SettlerThoughtStacks {
   public final SettlerThought thought;
   public int stacks;

   public SettlerThoughtStacks(SettlerThought thought, int stacks) {
      this.thought = thought;
      if (stacks < 1) {
         throw new IllegalArgumentException("Stacks must be at least 1");
      }

      this.stacks = stacks;
   }

   public SettlerThoughtStacks(SettlerThought thought) {
      this(thought, 1);
   }

   public GameMessage getDescription(HumanMob mob) {
      return this.thought.getStackedDescription(mob, this.stacks);
   }

   public int getHappinessModifier(HumanMob mob) {
      return this.thought.getStackedHappinessModifier(mob, this.stacks);
   }
}
