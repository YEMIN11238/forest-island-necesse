package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.Comparator;
import java.util.function.Function;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;
import necesse.entity.mobs.mobMovement.MobMovementPath;
import necesse.entity.mobs.mobMovement.MobMovementWhipPathRelative;

public class HeadsWhipStage<T extends MutantHydraBossMob> extends AINode<T> implements AttackStageInterface<T> {
   public TargetFinderAINode<T> targetFinderNode;
   public MutantHydraHeadType head;
   public Function<T, Integer> totalTimeGetter;
   public Function<T, Integer> shakeTimeGetter;
   public Function<T, Integer> attackCooldownGetter;
   public int maxRadius;
   public long currentEndTime;
   public long nextAttackTime;
   public Mob nextTarget;
   public float nextTargetAngle;
   public boolean waitingForArrive;

   public HeadsWhipStage(
      TargetFinderAINode<T> targetFinderNode,
      MutantHydraHeadType head,
      Function<T, Integer> totalTimeGetter,
      Function<T, Integer> shakeTimeGetter,
      Function<T, Integer> attackCooldownGetter,
      int maxRadius
   ) {
      this.targetFinderNode = targetFinderNode;
      this.head = head;
      this.totalTimeGetter = totalTimeGetter;
      this.shakeTimeGetter = shakeTimeGetter;
      this.attackCooldownGetter = attackCooldownGetter;
      this.maxRadius = maxRadius;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.currentEndTime = mob.getTime() + this.totalTimeGetter.apply(mob).intValue();
      this.nextAttackTime = 0L;
      this.waitingForArrive = false;
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.waitingForArrive) {
         MutantHydraHeadMob headMob = this.head.getter.apply(mob).getHeadMob();
         if (headMob != null && headMob.hasArrivedAtTarget()) {
            this.waitingForArrive = false;
            headMob.returnToIdle();
            this.nextAttackTime = mob.getTime() + this.attackCooldownGetter.apply(mob).intValue();
            this.nextTarget = null;
         }
      }

      if (!this.waitingForArrive && this.nextAttackTime <= mob.getTime()) {
         MutantHydraBossMob.HeadData headData = this.head.getter.apply(mob);
         MutantHydraHeadMob headMob = headData.getHeadMob();
         if (headMob != null) {
            float angleDelta = GameMath.getAngleDifference(this.head.endAngle, this.head.startAngle);
            if (this.nextTarget == null) {
               Point baseKeyObject = this.targetFinderNode.getBase(mob, blackboard);
               Point base = baseKeyObject != null ? baseKeyObject : new Point(mob.getX(), mob.getY());
               this.nextTarget = this.targetFinderNode
                  .streamPossibleTargets(mob, base, this.targetFinderNode.distance)
                  .filter(m -> this.targetFinderNode.validity.isValidTarget(this, mob, m, true))
                  .filter(m -> {
                     float distance = this.targetFinderNode.distance.getDistance(base, m);
                     return distance < this.maxRadius && distance < this.targetFinderNode.distance.getSearchDistance(mob, m);
                  })
                  .filter(m -> this.isTargetAtValidAngle(mob, m, angleDelta))
                  .findBestDistance(0, Comparator.comparingDouble(m -> this.targetFinderNode.distance.getDistance(base, m)))
                  .orElse(null);
               if (this.nextTarget != null) {
                  int shakeTime = this.shakeTimeGetter.apply(mob);
                  if (shakeTime > 0) {
                     headMob.startShakeAnimation.runAndSend(shakeTime);
                     this.nextAttackTime = mob.getTime() + shakeTime;
                  }

                  Float dir = GameMath.normalize(this.nextTarget.x - mob.x + headData.startXOffset, this.nextTarget.y - mob.y + headData.startYOffset);
                  this.nextTargetAngle = GameMath.getAngle(dir);
               } else {
                  this.nextAttackTime = mob.getTime() + this.attackCooldownGetter.apply(mob).intValue();
               }
            } else if (this.isTargetAtValidAngle(mob, this.nextTarget, angleDelta)) {
               headMob.setNewIdlePosition.set(false);
               Float dir = GameMath.normalize(this.nextTarget.x - mob.x + headData.startXOffset, this.nextTarget.y - mob.y + headData.startYOffset);
               float angle = GameMath.getAngle(dir);
               Float attackDir = GameMath.getAngleDir(angle + (dir.x < 0.0F ? 15 : -15));
               headMob.setMovement(
                  new MobMovementWhipPathRelative(
                     MobMovementPath.Mode.LIMITED, mob, 50, 0, mob, headData.startXOffset, headData.startYOffset, attackDir.x, attackDir.y, this.maxRadius
                  )
               );
               this.waitingForArrive = true;
            } else {
               headMob.setNewIdlePosition.set(false);
               Float dir = GameMath.normalize(this.nextTarget.x - mob.x + headData.startXOffset, this.nextTarget.y - mob.y + headData.startYOffset);
               Float attackDir = GameMath.getAngleDir(this.nextTargetAngle + (dir.x < 0.0F ? 15 : -15));
               headMob.setMovement(
                  new MobMovementWhipPathRelative(
                     MobMovementPath.Mode.LIMITED, mob, 50, 0, mob, headData.startXOffset, headData.startYOffset, attackDir.x, attackDir.y, this.maxRadius
                  )
               );
               this.waitingForArrive = true;
            }
         }
      }

      return mob.getTime() >= this.currentEndTime && this.nextTarget == null ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
   }

   public boolean isTargetAtValidAngle(T mob, Mob target, float angleDelta) {
      Float dir = GameMath.normalize(target.x - mob.x, target.y - mob.y);
      float angle = GameMath.getAngle(dir);
      float diff = GameMath.getAngleDifference(angle, this.head.startAngle);
      return diff > 0.0F && diff < angleDelta;
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
