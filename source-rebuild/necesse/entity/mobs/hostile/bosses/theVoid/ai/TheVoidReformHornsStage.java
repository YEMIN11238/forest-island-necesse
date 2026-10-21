package necesse.entity.mobs.hostile.bosses.theVoid.ai;

import necesse.entity.mobs.LevelMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidHornMob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;

public class TheVoidReformHornsStage<T extends TheVoidMob> extends AINode<T> implements AttackStageInterface<T> {
   public int fadeDuration = 500;
   public boolean alwaysReform;
   protected boolean completedFadeIn;

   public TheVoidReformHornsStage(boolean alwaysReform) {
      this.alwaysReform = alwaysReform;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.completedFadeIn) {
         return mob.getFadeAlpha() >= 1.0F ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
      }

      long timeSinceFadeStart = mob.getTimeSinceFadeStart();
      if (timeSinceFadeStart >= this.fadeDuration + 500) {
         mob.startFadeAbility.runAndSendFadeOut(this.fadeDuration);
         mob.setVulnerableAbility.runAndSend(false);

         for (LevelMob<TheVoidHornMob> lm : mob.spawnedHorns) {
            TheVoidHornMob horn = lm.get(mob.getLevel());
            if (horn != null) {
               horn.setBrokenAbility.runAndSend(false);
            }
         }

         mob.regenHornsAtPercentHealth = 0.0F;
         this.completedFadeIn = true;
      }

      return AINodeResult.RUNNING;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      if (mob.isAnyHornBroken() && (this.alwaysReform || !(mob.getHealthPercent() > mob.regenHornsAtPercentHealth))) {
         mob.startFadeAbility.runAndSendFadeIn(this.fadeDuration, 1000);
         mob.regenHornsAtPercentHealth = 0.0F;
         this.completedFadeIn = false;
      } else {
         this.completedFadeIn = true;
      }
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
