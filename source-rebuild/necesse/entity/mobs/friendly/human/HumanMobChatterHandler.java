package necesse.entity.mobs.friendly.human;

import java.util.Collection;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.job.JobWorkerChatter;
import necesse.entity.mobs.job.JobWorkerChatterHandler;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.inventory.item.Item;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.levelData.settlementData.settler.SettlerRecreationExhaustionManager;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;

public class HumanMobChatterHandler extends JobWorkerChatterHandler {
   public final HumanMob humanMob;

   public HumanMobChatterHandler(HumanMob humanMob, boolean shouldSetFacingDirection) {
      super(humanMob, shouldSetFacingDirection);
      this.humanMob = humanMob;
   }

   @Override
   public ZoneTester getChatterRestrictZone() {
      return this.humanMob.getJobRestrictZone();
   }

   @Override
   public Collection<Integer> getRecentFoodItemIDsEaten() {
      return this.humanMob.recentFoodItemIDsEaten;
   }

   @Override
   public float getChatterPositiveInteractionModifier() {
      return HumanShop.getHappinessPriceDiscountPercent(this.humanMob.getSettlerQualityOfLife(), this.humanMob.getSettlerHappiness());
   }

   @Override
   public void runAndSendOtherChatterThought(JobWorkerChatter target, int animTime) {
      this.humanMob.showOtherChatterThoughtAbility.runAndSend(target, animTime);
   }

   @Override
   public void runAndSendChatterMobThought(String mobStringID, int animTime) {
      this.humanMob.showMobThoughtAbility.runAndSend(mobStringID, animTime);
   }

   @Override
   public void runAndSendChatterItemThought(Item item, int animTime) {
      this.humanMob.showItemThoughtAbility.runAndSend(item, animTime);
   }

   @Override
   public void runAndSendLoveThought(int animTime) {
      this.humanMob.showLoveThoughtAbility.runAndSend(animTime);
   }

   @Override
   public void runAndSendExcitedThought(int animTime) {
      this.humanMob.showExcitedThoughtAbility.runAndSend(animTime);
   }

   @Override
   public void runAndSendDisagreeThought(int animTime) {
      this.humanMob.showDisagreeThoughtAbility.runAndSend(animTime);
   }

   @Override
   public void runAndSendShowExpression(FormExpressionWheel.Expression expression, int animTime) {
      this.humanMob.startExpressionAbility.runAndSend(expression, animTime);
   }

   @Override
   protected void onConversationGoing() {
      super.onConversationGoing();
      this.humanMob.regenRecreation(50.0F, 0.25F, SettlerRecreationExhaustionManager.SOCIALIZING_IDENTIFIER);
   }

   @Override
   public void endConversation(boolean becauseConversationIsOver, boolean wasPositive) {
      Mob chatterMob = this.getChatterMob();
      JobWorkerChatterHandler other = this.currentlyInteractingWith;
      if (becauseConversationIsOver && other != null && chatterMob instanceof HumanMob) {
         Mob otherChatter = other.getChatterMob();

         for (SettlerPersonality personality : ((HumanMob)chatterMob).getPersonalities()) {
            personality.onConversationEnded(otherChatter);
         }
      }

      super.endConversation(becauseConversationIsOver, wasPositive);
   }
}
