package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.EscapeAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.FollowerAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.friendly.HusbandryMob;
import necesse.entity.mobs.misc.ProcessMobHandler;
import necesse.entity.mobs.mobMovement.MobMovement;

public class HusbandryImpregnateWandererAI<T extends HusbandryMob> extends SelectorAINode<T> {
   public final HusbandryImpregnateWandererAI.HusbandryImpregnateAINode<T> impregnateAINode;
   public final EscapeAINode<T> escapeAINode;
   public final FollowerAINode<T> ropeFollowerAINode;
   public final WandererAINode<T> wandererAINode;

   public HusbandryImpregnateWandererAI(int wanderFrequency) {
      this.addChild(this.ropeFollowerAINode = new FollowerAINode<T>(320, 64) {
         public Mob getFollowingMob(T mob) {
            return mob.getRopeMob();
         }
      });
      this.addChild(this.escapeAINode = new EscapeAINode<T>() {
         public boolean shouldEscape(T mob, Blackboard<T> blackboard) {
            return mob.shouldEscape;
         }
      });
      this.addChild(this.impregnateAINode = new HusbandryImpregnateWandererAI.HusbandryImpregnateAINode<>());
      this.addChild(this.wandererAINode = new WandererAINode<>(wanderFrequency));
   }

   public static class HusbandryImpregnateAINode<T extends HusbandryMob> extends MoveTaskAINode<T> {
      public long nextCheckTime;
      public ProcessMobHandler<HusbandryMob, HusbandryMob> target;
      public HusbandryImpregnateWandererAI.MoveToTarget moveToTarget;
      public long nextMoveTime;
      public int pathsSinceProgress;
      public double lastPathDistToTarget;

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         if (this.target != null) {
            this.target.tickReserve();
         }

         return super.tick(mob, blackboard);
      }

      public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
         if (mob.isImpregnating() || mob.isReservedForImpregnating()) {
            blackboard.mover.stopMoving(mob);
            return AINodeResult.SUCCESS;
         }

         if (this.target != null && this.target.isValid()) {
            HusbandryImpregnateWandererAI.MoveToTarget last = this.moveToTarget;
            this.moveToTarget = this.target.getMoveToTile();
            if (this.moveToTarget == null || mob.collidesWith(this.target.target)) {
               blackboard.mover.stopMoving(mob);
               this.target.completeProgress();
               this.target = null;
               return AINodeResult.FAILURE;
            }

            if (this.moveToTarget.custom != null) {
               blackboard.mover.setCustomMovement(this, this.moveToTarget.custom);
               this.pathsSinceProgress = 0;
               return AINodeResult.RUNNING;
            }

            if (!this.moveToTarget.equals(last)) {
               this.nextMoveTime = 0L;
            }

            long currentTime = mob.getLocalTime();
            if (this.pathsSinceProgress > 5) {
               blackboard.mover.stopMoving(mob);
               this.target = null;
               return AINodeResult.FAILURE;
            }

            if (this.nextMoveTime <= currentTime) {
               this.nextMoveTime = currentTime + 5000L;
               if (mob.estimateCanMoveTo(this.moveToTarget.tileX, this.moveToTarget.tileY, false)) {
                  HusbandryImpregnateWandererAI.MoveToTarget temp = this.moveToTarget;
                  return this.moveToTileTask(temp.tileX, temp.tileY, null, path -> {
                     Point lastNode = path.result.getLastPathResult();
                     if (lastNode == null) {
                        this.pathsSinceProgress++;
                     } else {
                        double pathDistToTarget = lastNode.distance(path.result.target);
                        if (!(this.lastPathDistToTarget < 0.0) && pathDistToTarget != 0.0 && !(pathDistToTarget < this.lastPathDistToTarget)) {
                           this.pathsSinceProgress++;
                        } else {
                           this.pathsSinceProgress = 0;
                           this.lastPathDistToTarget = pathDistToTarget;
                        }
                     }

                     if (path.moveIfWithin(-1, -1, () -> this.nextMoveTime = 0L)) {
                        int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, 5000, 0.1F);
                        this.nextMoveTime = mob.getWorldEntity().getLocalTime() + nextPathTimeAdd;
                     }

                     return AINodeResult.RUNNING;
                  });
               }

               this.pathsSinceProgress++;
            }

            return AINodeResult.SUCCESS;
         } else {
            this.target = null;
            if (mob.canImpregnate() && !mob.isImpregnating() && !mob.isOnBirthingCooldown() && this.nextCheckTime <= mob.getTime()) {
               this.nextCheckTime = mob.getTime() + GameRandom.globalRandom.getIntBetween(20, 30) * 1000L;
               List<HusbandryMob> nearbyMobs = mob.getNearbyHusbandryMobs();
               if (nearbyMobs.size() <= HusbandryMob.maxCloseMobsToBirth) {
                  ArrayList<HusbandryMob> validImpregnateMobs = new ArrayList<>(nearbyMobs.size());

                  for (HusbandryMob other : nearbyMobs) {
                     if (other.canBirth()
                        && !other.isOnBirthingCooldown()
                        && mob.canImpregnateMob(other)
                        && mob.estimateCanMoveTo(other.getTileX(), other.getTileY(), false)
                        && (other.impregnateReservable == null || other.impregnateReservable.isAvailable(mob))) {
                        validImpregnateMobs.add(other);
                     }
                  }

                  if (!validImpregnateMobs.isEmpty()) {
                     HusbandryMob impregnateMob = GameRandom.globalRandom.getOneOf(validImpregnateMobs);
                     this.target = new ProcessMobHandler<HusbandryMob, HusbandryMob>(mob, impregnateMob, impregnateMob.impregnateReservable) {
                        @Override
                        public boolean isValid() {
                           return !this.target.removed() && this.target.canBirth() && this.mob.canImpregnate() && this.mob.canImpregnateMob(this.target);
                        }

                        @Override
                        public void completeProgress() {
                           this.mob.impregnateMobAbility.runAndSend(this);
                        }

                        @Override
                        public int getTimeItTakesInMilliseconds() {
                           return 10000;
                        }
                     };
                     this.target.tickReserve();
                     return AINodeResult.SUCCESS;
                  }
               }
            }

            return AINodeResult.FAILURE;
         }
      }
   }

   public static class MoveToTarget {
      public final MobMovement custom;
      public final int tileX;
      public final int tileY;

      public MoveToTarget(int tileX, int tileY) {
         this.custom = null;
         this.tileX = tileX;
         this.tileY = tileY;
      }

      public MoveToTarget(MobMovement custom) {
         this.custom = custom;
         this.tileX = -1;
         this.tileY = -1;
      }

      public boolean equals(HusbandryImpregnateWandererAI.MoveToTarget other) {
         if (this == other) {
            return true;
         } else if (other == null) {
            return false;
         } else if (!Objects.equals(this.custom, other.custom)) {
            return false;
         } else {
            return this.custom != null ? true : this.tileX == other.tileX && this.tileY == other.tileY;
         }
      }

      @Override
      public boolean equals(Object obj) {
         return obj instanceof HusbandryImpregnateWandererAI.MoveToTarget ? this.equals((HusbandryImpregnateWandererAI.MoveToTarget)obj) : super.equals(obj);
      }
   }
}
