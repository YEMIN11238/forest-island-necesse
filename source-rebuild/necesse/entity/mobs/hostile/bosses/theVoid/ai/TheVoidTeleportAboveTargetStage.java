package necesse.entity.mobs.hostile.bosses.theVoid.ai;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidClawMob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;
import necesse.entity.mobs.mobMovement.MobMovementRelative;

public class TheVoidTeleportAboveTargetStage<T extends TheVoidMob> extends AINode<T> implements AttackStageInterface<T> {
   private Mob target;
   private final int fadeDuration = 500;
   public boolean completedFadeIn;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.target == null) {
         return AINodeResult.SUCCESS;
      }

      if (this.completedFadeIn) {
         return mob.getFadeAlpha() >= 1.0F ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
      }

      long timeSinceFadeStart = mob.getTimeSinceFadeStart();
      if (timeSinceFadeStart >= 1000L) {
         mob.startFadeAbility.runAndSendFadeOut(500);
         mob.setPos(this.target.x, this.target.y - 400.0F, true);
         blackboard.mover.setCustomMovement(this, new MobMovementRelative(this.target, 0.0F, -400.0F));
         mob.sendMovementPacket(true);

         for (TheVoidClawMob spawnedClaw : mob.spawnedClaws) {
            spawnedClaw.teleportToMasterIfIdle();
         }

         this.completedFadeIn = true;
      }

      return AINodeResult.RUNNING;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.target = blackboard.getObject(Mob.class, "currentTarget");
      if (this.target != null) {
         mob.startFadeAbility.runAndSendFadeIn(500, 1000);
         this.completedFadeIn = false;
      }
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
