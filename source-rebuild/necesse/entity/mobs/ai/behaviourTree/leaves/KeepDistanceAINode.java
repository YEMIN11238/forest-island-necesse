package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.MovedRectangle;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.event.AIWasHitEvent;
import necesse.entity.mobs.mobMovement.MobMovementRelative;

public class KeepDistanceAINode<T extends Mob> extends MoveTaskAINode<T> {
   public String targetKey;
   public boolean moveAwayFromHits = true;
   public int maxTargetDistance;
   public String chaserTargetKey = "chaserTarget";
   protected boolean movingAway = false;

   public KeepDistanceAINode(String targetKey, int maxTargetDistance) {
      this.targetKey = targetKey;
      this.maxTargetDistance = maxTargetDistance;
   }

   public KeepDistanceAINode(int maxTargetDistance) {
      this("currentTarget", maxTargetDistance);
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      blackboard.onEvent("resetTarget", e -> this.getBlackboard().put(this.chaserTargetKey, null));
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
      blackboard.put(this.chaserTargetKey, null);
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, this.targetKey);
      boolean forceMoveAway = false;
      if (this.moveAwayFromHits) {
         for (AIWasHitEvent e : blackboard.getLastHits()) {
            Mob attacker = e.event.attacker != null ? e.event.attacker.getAttackOwner() : null;
            if (attacker != null) {
               target = attacker;
               forceMoveAway = true;
               break;
            }
         }
      }

      if (target != null) {
         float distance = mob.getDistance(target);
         if (distance < this.maxTargetDistance || forceMoveAway) {
            Float targetDir = GameMath.normalize(mob.x - target.x, mob.y - target.y);
            float angle = GameMath.getAngle(targetDir);
            int totalDirections = 8;
            int totalDirectionsAngle = 360 / totalDirections;
            angle = GameMath.fixAngle(angle + totalDirectionsAngle / 2.0F);
            int dir = (int)(angle / totalDirectionsAngle);
            Float finalDir = new Float();

            for (int i = 0; i < totalDirections / 2; i++) {
               int currentDir = dir - i;
               finalDir = GameMath.getAngleDir(currentDir * totalDirectionsAngle);
               int targetXPos = (int)(mob.x + finalDir.x * this.maxTargetDistance);
               int targetYPos = (int)(mob.y + finalDir.y * this.maxTargetDistance);
               if (!mob.getLevel().collides(new MovedRectangle(mob, targetXPos, targetYPos), mob.getLevelCollisionFilter())) {
                  break;
               }

               if (i != 0) {
                  currentDir = dir + i;
                  finalDir = GameMath.getAngleDir(currentDir * totalDirectionsAngle);
                  targetXPos = (int)(mob.x + finalDir.x * this.maxTargetDistance);
                  targetYPos = (int)(mob.y + finalDir.y * this.maxTargetDistance);
                  if (!mob.getLevel().collides(new MovedRectangle(mob, targetXPos, targetYPos), mob.getLevelCollisionFilter())) {
                     break;
                  }
               }
            }

            float walkAwayDistance = (forceMoveAway ? distance : 0.0F) + this.maxTargetDistance + mob.moveAccuracy;
            blackboard.mover.setCustomMovement(this, new MobMovementRelative(target, finalDir.x * walkAwayDistance, finalDir.y * walkAwayDistance));
            this.movingAway = true;
         }
      } else {
         this.movingAway = false;
      }

      if (this.movingAway) {
         this.getBlackboard().put(this.chaserTargetKey, target);
         return AINodeResult.SUCCESS;
      } else {
         return AINodeResult.FAILURE;
      }
   }
}
