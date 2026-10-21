package necesse.level.maps.levelData.settlementData.settler.thoughts;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.friendly.human.HumanMob;

public class SimpleScalingHappinessSettlerThought extends SettlerThought {
   public GameMessage description;
   public int firstStackHappinessModifier;
   public int nextStackDeltaHappinessModifier;
   public int eachStackHappinessModifier;
   protected int maxStacks = -1;

   public SimpleScalingHappinessSettlerThought(
      GameMessage description, int firstStackHappinessModifier, int nextStackDeltaHappinessModifier, int eachStackHappinessModifier
   ) {
      this.description = description;
      this.firstStackHappinessModifier = firstStackHappinessModifier;
      this.nextStackDeltaHappinessModifier = nextStackDeltaHappinessModifier;
      this.eachStackHappinessModifier = eachStackHappinessModifier;
   }

   public SimpleScalingHappinessSettlerThought(
      String category, String key, int firstStackHappinessModifier, int nextStackDeltaHappinessModifier, int eachStackHappinessModifier
   ) {
      this(new LocalMessage(category, key), firstStackHappinessModifier, nextStackDeltaHappinessModifier, eachStackHappinessModifier);
   }

   @Override
   public int getMaxStacks(HumanMob mob) {
      if (this.maxStacks == -1) {
         if (this.eachStackHappinessModifier == 0
            && this.firstStackHappinessModifier != 0
            && (this.firstStackHappinessModifier <= 0 || this.nextStackDeltaHappinessModifier < 0)
            && (this.firstStackHappinessModifier >= 0 || this.nextStackDeltaHappinessModifier > 0)) {
            this.maxStacks = Math.abs(this.firstStackHappinessModifier) / Math.abs(this.nextStackDeltaHappinessModifier);
         } else {
            this.maxStacks = Integer.MAX_VALUE;
         }
      }

      return this.maxStacks;
   }

   @Override
   protected GameMessage getDescription(HumanMob mob) {
      return this.description;
   }

   @Override
   public int getStackedHappinessModifier(HumanMob mob, int stacks) {
      stacks = Math.min(stacks, this.getMaxStacks(mob));
      int totalHappiness = this.firstStackHappinessModifier + this.eachStackHappinessModifier * stacks;
      if (this.nextStackDeltaHappinessModifier != 0 && stacks > 1) {
         int nextModifier = this.firstStackHappinessModifier + this.nextStackDeltaHappinessModifier;

         for (int i = 1; i < stacks; i++) {
            totalHappiness += nextModifier;
            nextModifier += this.nextStackDeltaHappinessModifier;
            if (this.firstStackHappinessModifier != 0 && Math.signum(this.firstStackHappinessModifier) != Math.signum(nextModifier)) {
               break;
            }
         }
      }

      return totalHappiness;
   }

   @Override
   protected int getHappinessModifier(HumanMob mob) {
      return this.eachStackHappinessModifier;
   }

   public void printDebugValues(HumanMob mob) {
      int maxStacks = this.getMaxStacks(mob);
      int shownStacks = maxStacks;
      if (shownStacks == Integer.MAX_VALUE) {
         if (this.firstStackHappinessModifier != 0
            && (this.firstStackHappinessModifier <= 0 || this.nextStackDeltaHappinessModifier < 0)
            && (this.firstStackHappinessModifier >= 0 || this.nextStackDeltaHappinessModifier > 0)) {
            shownStacks = Math.abs(this.firstStackHappinessModifier) / Math.abs(this.nextStackDeltaHappinessModifier) + 5;
         } else {
            shownStacks = 9;
         }
      }

      System.out
         .println(
            "Happiness values for formula: "
               + this.firstStackHappinessModifier
               + " + ("
               + this.firstStackHappinessModifier
               + (
                  this.nextStackDeltaHappinessModifier < 0
                     ? " - " + Math.abs(this.nextStackDeltaHappinessModifier)
                     : " + " + this.nextStackDeltaHappinessModifier
               )
               + " * (i - 1)) + "
               + this.eachStackHappinessModifier
               + " * stacks (max "
               + maxStacks
               + ")"
         );

      for (int stacks = 1; stacks <= shownStacks; stacks++) {
         System.out.println("\tStacks: " + stacks + ", Total happiness modifier: " + this.getStackedHappinessModifier(mob, stacks));
      }
   }
}
