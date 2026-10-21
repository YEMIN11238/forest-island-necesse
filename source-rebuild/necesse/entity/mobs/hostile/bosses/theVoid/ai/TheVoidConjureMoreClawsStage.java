package necesse.entity.mobs.hostile.bosses.theVoid.ai;

import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidClawMob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;

public class TheVoidConjureMoreClawsStage<T extends TheVoidMob> extends AINode<T> implements AttackStageInterface<T> {
   private final int fadeDuration = 500;
   public boolean completedFadeIn;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.completedFadeIn) {
         return mob.getFadeAlpha() >= 1.0F ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
      }

      long timeSinceFadeStart = mob.getTimeSinceFadeStart();
      if (timeSinceFadeStart >= 1000L) {
         mob.startFadeAbility.runAndSendFadeOut(500);

         while (mob.spawnedClaws.size() < 4) {
            mob.spawnClaw();
         }

         for (TheVoidClawMob spawnedClaw : mob.spawnedClaws) {
            spawnedClaw.teleportToMasterIfIdle();
         }

         this.completedFadeIn = true;
      }

      return AINodeResult.RUNNING;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      if (mob.spawnedClaws.size() < 4) {
         mob.startFadeAbility.runAndSendFadeIn(500, 1000);
         this.completedFadeIn = false;
      } else {
         this.completedFadeIn = true;
      }
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
