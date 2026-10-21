package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.geom.Point2D.Float;
import java.util.function.Supplier;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.AttackAnimMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.event.ConfuseWanderAIEvent;
import necesse.entity.mobs.ai.behaviourTree.leaves.ConfusedWandererAINode;

public abstract class ConfusedPlayerChaserWandererAI<T extends AttackAnimMob> extends PlayerChaserWandererAI<T> {
   public final ConfusedWandererAINode<T> confusedWandererNode;
   protected boolean wanderAfterAttack = false;

   public ConfusedPlayerChaserWandererAI(
      Supplier<Boolean> shouldEscape, int searchDistance, int shootDistance, int wanderFrequency, boolean smartPositioning, boolean changePositionOnHit
   ) {
      super(shouldEscape, searchDistance, shootDistance, wanderFrequency, smartPositioning, changePositionOnHit);
      this.addChildFirst(new AINode<T>() {
         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
            blackboard.onEvent("wanderAfterAttack", e -> ConfusedPlayerChaserWandererAI.this.wanderAfterAttack = true);
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            if (ConfusedPlayerChaserWandererAI.this.wanderAfterAttack && !mob.isAttacking) {
               ConfusedPlayerChaserWandererAI.this.wanderAfterAttack = false;
               ConfusedPlayerChaserWandererAI.this.runWanderAfterAttack(mob, blackboard);
            }

            return AINodeResult.FAILURE;
         }
      });
      this.addChildFirst(this.confusedWandererNode = new ConfusedWandererAINode<>());
   }

   protected int getRandomConfuseTime() {
      return GameRandom.globalRandom.getChance(0.1F) ? GameRandom.globalRandom.getIntBetween(2000, 4000) : GameRandom.globalRandom.getIntBetween(1000, 1500);
   }

   public void runWanderAfterAttack(T mob, Blackboard<T> blackboard) {
      if (mob.attackDir != null) {
         float attackAngle = GameMath.getAngle(mob.attackDir);
         float runAwayAngle;
         if (GameRandom.globalRandom.nextBoolean()) {
            runAwayAngle = GameRandom.globalRandom.getFloatBetween(attackAngle - 90.0F, attackAngle - 110.0F);
         } else {
            runAwayAngle = GameRandom.globalRandom.getFloatBetween(attackAngle + 90.0F, attackAngle + 110.0F);
         }

         runAwayAngle = GameMath.fixAngle(runAwayAngle);
         Float runAwayDir = GameMath.getAngleDir(runAwayAngle);
         int confuseTime = this.getRandomConfuseTime();
         blackboard.submitEvent("confuseWander", new ConfuseWanderAIEvent(confuseTime, runAwayDir));
      }
   }
}
