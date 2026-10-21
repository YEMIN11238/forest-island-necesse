package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.mobMovement.MobMovementCharge;

public class ChargingCirclingChaserAINode<T extends Mob> extends AINode<T> {
   public String targetKey = "currentTarget";
   public String chaserTargetKey = "chaserTarget";
   public int circlingRange;
   public int nextAngleOffset;
   public boolean backOffOnReset = true;
   public boolean resetOnOutsideCircleRange = false;
   public int circlingTicks = 1;
   public int offsetChasingBy = 0;
   protected Mob chargingTarget;
   protected int nextAttack;
   protected float circlingDirection = 1.0F;
   private int startMoveAccuracy;

   public ChargingCirclingChaserAINode(int circlingRange, int nextAngleOffset) {
      this.circlingRange = circlingRange;
      this.nextAngleOffset = nextAngleOffset;
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      this.startMoveAccuracy = mob.moveAccuracy;
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
      blackboard.put(this.chaserTargetKey, null);
   }

   @Override
   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, this.targetKey);
      if (target != null && target.isSamePlace(mob)) {
         if (this.chargingTarget != null) {
            if (!this.chargingTarget.removed() && this.chargingTarget.canBeTargeted(mob, null)) {
               if (!blackboard.mover.isCurrentlyMovingFor(this)) {
                  this.startCharge(mob, blackboard, this.chargingTarget);
               }

               if (mob.getDistance(this.chargingTarget) < this.circlingRange / 3.0F) {
                  float currentAngle = GameMath.getAngle(new Float(mob.dx, mob.dy));
                  float targetAngle = GameMath.getAngle(new Float(target.x - mob.x, target.y - mob.y));
                  float diff = GameMath.getAngleDifference(currentAngle, targetAngle);
                  if (Math.abs(diff) >= 40.0F || mob.dx == 0.0F && mob.dy == 0.0F) {
                     this.startCircling(mob, blackboard, target, this.circlingTicks);
                  }
               }
            } else {
               this.startCircling(mob, blackboard, target, this.circlingTicks);
            }

            blackboard.put(this.chaserTargetKey, this.chargingTarget);
         } else {
            if (!blackboard.mover.isCurrentlyMovingFor(this)
               || mob.hasArrivedAtTarget()
               || this.resetOnOutsideCircleRange && mob.getDistance(target) > this.circlingRange) {
               this.nextAttack--;
               if (this.nextAttack <= 0) {
                  this.startCharge(mob, blackboard, target);
               } else {
                  float currentAngle = GameMath.getAngle(new Float(mob.x - target.x, mob.y - target.y));
                  float nextAngle = currentAngle + GameRandom.globalRandom.getFloatBetween(0.0F, this.nextAngleOffset * this.circlingDirection);
                  this.findNewPosition(nextAngle, mob, blackboard, target);
               }
            }

            blackboard.put(this.chaserTargetKey, target);
         }

         return AINodeResult.SUCCESS;
      } else {
         this.chargingTarget = null;
         return AINodeResult.FAILURE;
      }
   }

   public void startCharge(T mob, Blackboard<T> blackboard, Mob target) {
      mob.moveAccuracy = 5;
      if (this.offsetChasingBy != 0) {
         MobMovementCharge movement = new MobMovementCharge(target, this.offsetChasingBy);
         blackboard.mover.setCustomMovement(this, movement);
      } else {
         blackboard.mover.setMobTarget(this, target);
      }

      this.chargingTarget = target;
   }

   public void startCircling(T mob, Blackboard<T> blackboard, Mob target, int circlingTicks) {
      this.startCircling(mob, blackboard, target, circlingTicks, 0.0F);
   }

   public void startCircling(T mob, Blackboard<T> blackboard, Mob target, int circlingTicks, float direction) {
      float escapeAngle;
      if (this.backOffOnReset) {
         escapeAngle = GameMath.getAngle(new Float(mob.x - target.x, mob.y - target.y));
      } else {
         escapeAngle = GameMath.getAngle(new Float(target.x - mob.x, target.y - mob.y));
      }

      this.findNewPosition((int)escapeAngle, mob, blackboard, target);
      this.chargingTarget = null;
      this.nextAttack = circlingTicks;
      this.circlingDirection = Math.signum(direction);
      if (this.circlingDirection == 0.0F) {
         float currentAngle = GameMath.getAngle(new Float(mob.dx, mob.dy));
         float angleDifference = GameMath.getAngleDifference(escapeAngle, currentAngle);
         this.circlingDirection = angleDifference > 0.0F ? 1.0F : -1.0F;
      }
   }

   public void findNewPosition(float escapeAngle, T mob, Blackboard<T> blackboard, Mob target) {
      float nx = (float)Math.cos(Math.toRadians(escapeAngle));
      float ny = (float)Math.sin(Math.toRadians(escapeAngle));
      mob.moveAccuracy = this.startMoveAccuracy;
      blackboard.mover.directMoveTo(this, (int)(target.x + nx * this.circlingRange), (int)(target.y + ny * this.circlingRange));
   }

   public void fixMoveAccuracy() {
      this.mob().moveAccuracy = this.startMoveAccuracy;
   }
}
