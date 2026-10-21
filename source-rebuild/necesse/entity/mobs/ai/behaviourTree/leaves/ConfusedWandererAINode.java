package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.event.ConfuseWanderAIEvent;
import necesse.entity.mobs.mobMovement.MobMovementCircle;
import necesse.entity.mobs.mobMovement.MobMovementCircleLevelPos;

public class ConfusedWandererAINode<T extends Mob> extends AINode<T> {
   public long confusionTimer = 0L;
   public boolean timerIsBasedOnWorld = false;
   public boolean collisionCausesDirectionChange = true;
   public float hitsCausesConfusionChance = 0.5F;
   public int hitCausesConfusionCooldown = 2000;
   protected long nextHitCausesConfusionTime = 0L;
   public int changeDirectionMinTime = 1000;
   public int changeDirectionMaxTime = 3000;
   public int minCircleRadius = 64;
   public int maxCircleRadius = 192;
   protected int changeDirectionTimer;
   protected Float nextDirection;

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      blackboard.onWasHit(
         e -> {
            if (this.hitsCausesConfusionChance > 0.0F
               && this.nextHitCausesConfusionTime <= mob.getTime()
               && GameRandom.globalRandom.getChance(this.hitsCausesConfusionChance)) {
               Float hitDir = GameMath.normalize(e.event.knockbackX, e.event.knockbackY);
               Float perpDir = GameMath.getPerpendicularDir(hitDir);
               float perpAngle = GameMath.getAngle(perpDir);
               float nextAngle = GameMath.fixAngle(perpAngle + GameRandom.globalRandom.getIntBetween(-45, 45));
               Float finalDir = GameMath.getAngleDir(nextAngle);
               if (GameRandom.globalRandom.nextBoolean()) {
                  finalDir = new Float(-finalDir.x, -finalDir.y);
               }

               int confusionTime = GameRandom.globalRandom.getIntBetween(750, 1500);
               ConfuseWanderAIEvent event = new ConfuseWanderAIEvent(confusionTime, finalDir);
               this.handleEvent(event);
               this.nextHitCausesConfusionTime = mob.getTime() + confusionTime + this.hitCausesConfusionCooldown;
            }
         }
      );
      blackboard.onEvent("confuseWander", e -> {
         if (e instanceof ConfuseWanderAIEvent) {
            this.handleEvent((ConfuseWanderAIEvent)e);
         } else {
            this.confusionTimer = 5000L;
         }
      });
   }

   protected void handleEvent(ConfuseWanderAIEvent event) {
      if (event.canDecreaseTimer) {
         this.confusionTimer = event.confusionTimer;
      } else {
         this.confusionTimer = Math.max(this.confusionTimer, event.confusionTimer);
      }

      this.changeDirectionTimer = 0;
      this.nextDirection = event.nextDirection;
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      boolean isConfused;
      if (this.timerIsBasedOnWorld) {
         isConfused = mob.getTime() >= this.confusionTimer;
      } else {
         isConfused = this.confusionTimer > 0L;
         this.confusionTimer -= 50L;
      }

      if (isConfused) {
         this.changeDirectionTimer -= 50;
         Float dir = this.nextDirection;
         if (this.collisionCausesDirectionChange && mob.isAccelerating()) {
            Float moveDir = GameMath.normalize(mob.moveX, mob.moveY);
            int range = 10;
            Rectangle collision = mob.getCollision((int)(mob.x + moveDir.x * range), (int)(mob.y + moveDir.y * range));
            if (mob.getLevel().collides(collision, mob.getLevelCollisionFilter())) {
               float lastMoveAngle = GameMath.getAngle(moveDir);
               float newMoveAngle = GameMath.fixAngle(lastMoveAngle + GameRandom.globalRandom.getIntBetween(135, 225));
               dir = GameMath.getAngleDir(newMoveAngle);
               this.changeDirectionTimer = 0;
            }
         }

         if (this.changeDirectionTimer <= 0 || !blackboard.mover.isCurrentlyMovingFor(this)) {
            this.changeDirectionTimer = GameRandom.globalRandom.getIntBetween(this.changeDirectionMinTime, this.changeDirectionMaxTime);
            this.changeMovement(mob, blackboard, dir);
         }

         return AINodeResult.SUCCESS;
      } else {
         if (blackboard.mover.isCurrentlyMovingFor(this)) {
            blackboard.mover.stopMoving(mob);
         }

         return AINodeResult.FAILURE;
      }
   }

   protected void changeMovement(T mob, Blackboard<T> blackboard, Float dir) {
      if (dir == null && mob.isAccelerating()) {
         Float moveDir = GameMath.normalize(mob.moveX, mob.moveY);
         float moveAngle = GameMath.getAngle(moveDir);
         float nextAngle = GameMath.fixAngle(moveAngle + GameRandom.globalRandom.getIntBetween(90, 270));
         dir = GameMath.getAngleDir(nextAngle);
      }

      int circleRadius = GameRandom.globalRandom.getIntBetween(this.minCircleRadius, this.maxCircleRadius);
      boolean reversed = GameRandom.globalRandom.nextBoolean();
      Float centerDir;
      if (dir != null) {
         centerDir = GameMath.getPerpendicularDir(dir);
         if (reversed) {
            centerDir = new Float(-centerDir.x, -centerDir.y);
         }
      } else {
         centerDir = GameMath.getAngleDir(GameRandom.globalRandom.nextInt(360));
      }

      Float centerPos = new Float(mob.x + centerDir.x * circleRadius, mob.y + centerDir.y * circleRadius);
      float speed = MobMovementCircle.convertToRotSpeed(circleRadius, mob.getSpeed());
      blackboard.mover.setCustomMovement(this, new MobMovementCircleLevelPos(mob, centerPos.x, centerPos.y, circleRadius, speed, reversed));
   }
}
