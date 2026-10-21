package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.util.Comparator;
import necesse.engine.util.GameRandom;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.util.SummonTargetFinderDistance;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;

public class SummonTargetFinderAINode<T extends Mob> extends MoveTaskAINode<T> {
   public TargetFinderDistance<T> distance;
   public String currentTargetKey;
   public int loseTargetTimer;
   public int noTargetFoundTimer;
   public int loseTargetMinCooldown = 2000;
   public int loseTargetMaxCooldown = 4000;

   public SummonTargetFinderAINode(TargetFinderDistance<T> distance, String currentTargetKey) {
      this.distance = distance;
      this.currentTargetKey = currentTargetKey;
      this.startLoseTargetTimer();
   }

   public SummonTargetFinderAINode(TargetFinderDistance<T> distance) {
      this(distance, "currentTarget");
   }

   public SummonTargetFinderAINode(int searchDistance) {
      this(new SummonTargetFinderDistance<>(searchDistance));
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      Mob lastTarget = blackboard.getObject(Mob.class, this.currentTargetKey);
      Mob newTarget = lastTarget;
      boolean addTargetLostDistance = false;
      int lastSearchDistance = this.distance.searchDistance;
      ItemAttackerMob owner = mob.getFollowingItemAttacker();
      Point base = owner == null ? mob.getPositionPoint() : owner.getPositionPoint();
      Mob focusTarget = blackboard.getObject(Mob.class, "focusTarget");

      try {
         if (newTarget == null && this.noTargetFoundTimer >= 0) {
            this.noTargetFoundTimer -= 50;
            return AINodeResult.FAILURE;
         }

         if (newTarget != null && this.loseTargetMinCooldown >= 0 && this.loseTargetMaxCooldown >= 0) {
            this.loseTargetTimer -= 50;
            if (this.loseTargetTimer <= 0) {
               newTarget = null;
               addTargetLostDistance = true;
               this.startLoseTargetTimer();
            }
         }

         if (newTarget != null
            && (
               !this.isValidTarget(mob, owner, newTarget, focusTarget)
                  || this.distance.getDistance(base, newTarget) > this.distance.getTargetLostDistance(mob, newTarget)
                  || !this.checkCanMoveTo(owner, newTarget)
            )) {
            newTarget = null;
            addTargetLostDistance = true;
         }

         if (addTargetLostDistance) {
            this.distance.searchDistance = this.distance.searchDistance + this.distance.targetLostAddedDistance;
         }

         if (focusTarget != null
            && newTarget != focusTarget
            && this.isValidTarget(mob, owner, focusTarget, focusTarget)
            && this.distance.getDistance(base, focusTarget) < this.distance.getSearchDistance(mob, focusTarget)
            && this.checkCanMoveTo(owner, focusTarget)) {
            newTarget = focusTarget;
         }

         if (newTarget == null) {
            GameAreaStream<? extends Mob> targetStream = this.streamPossibleTargets(owner, mob, base, this.distance);
            if (targetStream != null) {
               newTarget = targetStream.filter(
                     m -> this.isValidTarget(mob, owner, m, focusTarget) && this.distance.getDistance(base, m) < this.distance.getSearchDistance(mob, m)
                  )
                  .filter(m -> this.checkCanMoveTo(owner, m))
                  .findBestDistance(0, Comparator.comparingInt(m -> (int)mob.getDistance(m)))
                  .orElse(null);
            }
         }

         if (lastTarget != newTarget) {
            blackboard.put(this.currentTargetKey, newTarget);
         }

         return newTarget != null ? AINodeResult.SUCCESS : AINodeResult.FAILURE;
      } finally {
         this.distance.searchDistance = lastSearchDistance;
      }
   }

   public void startLoseTargetTimer() {
      this.loseTargetTimer = GameRandom.globalRandom.getIntBetween(this.loseTargetMinCooldown, this.loseTargetMaxCooldown);
   }

   public GameAreaStream<? extends Mob> streamPossibleTargets(ItemAttackerMob owner, T mob, Point base, TargetFinderDistance<T> distance) {
      return owner != null && owner.isHostile
         ? TargetFinderAINode.streamPlayersAndHumans(mob, base, distance)
         : distance.streamMobsInRange(base, mob).filter(m -> m != null && !m.removed() && m.isVisible()).filter(m -> m.isHostile);
   }

   public boolean isValidTarget(T mob, ItemAttackerMob owner, Mob target, Mob focusTarget) {
      if (target != null
         && !target.removed()
         && target.isVisible()
         && mob.isSamePlace(target)
         && (target == focusTarget || target.canBeTargeted(mob, owner == null ? null : owner.getPvPOwner()))) {
         Point base = owner == null ? mob.getPositionPoint() : owner.getPositionPoint();
         if (target.getLevel().getStaticLightLevelFloat(target) <= 0.0F && !target.isBoss() && target.getDistance(base.x, base.y) >= 256.0F) {
            return false;
         }

         boolean hostileOwner = owner != null && owner.isHostile;
         return target.isHostile || hostileOwner || target == focusTarget;
      } else {
         return false;
      }
   }

   public boolean checkCanMoveTo(ItemAttackerMob owner, Mob target) {
      return owner == null ? true : owner.serverFollowersManager.followerTargetCooldowns.canMoveTo(target);
   }
}
