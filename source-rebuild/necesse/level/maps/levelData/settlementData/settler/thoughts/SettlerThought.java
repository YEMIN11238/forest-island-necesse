package necesse.level.maps.levelData.settlementData.settler.thoughts;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IDDataContainer;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;

public abstract class SettlerThought implements IDDataContainer {
   public final IDData idData = new IDData();
   protected SettlerPersonality personalitySubmitter;

   @Override
   public IDData getIDData() {
      return this.idData;
   }

   public SettlerThought setPersonalitySubmitter(SettlerPersonality personality) {
      this.personalitySubmitter = personality;
      return this;
   }

   public boolean shouldShowThought(HumanMob mob) {
      return true;
   }

   public boolean shouldClearIfDoesntHavePersonalitySubmitter() {
      return true;
   }

   public SettlerPersonality getPersonalitySubmitter() {
      return this.personalitySubmitter;
   }

   protected abstract GameMessage getDescription(HumanMob var1);

   protected abstract int getHappinessModifier(HumanMob var1);

   public int getMaxStacks(HumanMob mob) {
      return Integer.MAX_VALUE;
   }

   public GameMessage getStackedDescription(HumanMob mob, int stacks) {
      stacks = Math.min(stacks, this.getMaxStacks(mob));
      GameMessageBuilder builder = new GameMessageBuilder();
      if (this.personalitySubmitter != null) {
         builder.append("[").append(this.personalitySubmitter.getName()).append("] ");
      }

      if (stacks <= 1) {
         builder.append(this.getDescription(mob));
      } else {
         builder.append(this.getDescription(mob)).append(" x" + stacks);
      }

      return builder;
   }

   public int getStackedHappinessModifier(HumanMob mob, int stacks) {
      stacks = Math.min(stacks, this.getMaxStacks(mob));
      return this.getHappinessModifier(mob) * stacks;
   }
}
