package necesse.entity.mobs.hostile.bosses.theVoid.ai;

import java.awt.geom.Point2D.Float;
import java.util.Comparator;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidClawMob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;

public class TheVoidMultiClawSlamStage<T extends TheVoidMob> extends AINode<T> implements AttackStageInterface<T> {
   public float randomMoveRangeAroundTarget;
   public int totalTime;
   public float slamsPerSecAtNoHealth;
   public float slamsPerSecAtFullHealth;
   protected long startTime;
   protected float nextSlamBuffer;

   public TheVoidMultiClawSlamStage(float randomMoveRangeAroundTarget, int totalTime, float slamsPerSecAtNoHealth, float slamsPerSecAtFullHealth) {
      this.randomMoveRangeAroundTarget = randomMoveRangeAroundTarget;
      this.totalTime = totalTime;
      this.slamsPerSecAtNoHealth = slamsPerSecAtNoHealth;
      this.slamsPerSecAtFullHealth = slamsPerSecAtFullHealth;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      long timeSinceStart = mob.getTime() - this.startTime;
      if (timeSinceStart >= this.totalTime) {
         return AINodeResult.SUCCESS;
      }

      if (!blackboard.mover.isCurrentlyMovingFor(this)) {
         this.findNextPosition(mob, blackboard);
      }

      float slamsPerSec = GameMath.lerp(mob.getHealthPercent(), this.slamsPerSecAtNoHealth, this.slamsPerSecAtFullHealth);
      float slamsPerTick = slamsPerSec / 20.0F;
      this.nextSlamBuffer += slamsPerTick;

      while (this.nextSlamBuffer >= 1.0F) {
         this.nextSlamBuffer--;
         Mob target = blackboard.getObject(Mob.class, "currentTarget");
         if (target == null) {
            this.nextSlamBuffer = 0.0F;
            break;
         }

         TheVoidClawMob claw = mob.spawnedClaws
            .stream()
            .filter(TheVoidClawMob::isIdle)
            .min(Comparator.comparingDouble(c -> c.getDistance(target)))
            .orElse(null);
         if (claw != null) {
            float offsetX = GameMath.limit(target.dx * 10.0F, -50.0F, 50.0F);
            float offsetY = GameMath.limit(target.dy * 10.0F, -50.0F, 50.0F);
            claw.flyToTargetAndSlam(target, offsetX, offsetY, 2000, 300, 250, 1000, false);
         }
      }

      return AINodeResult.RUNNING;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.startTime = mob.getTime();
      this.nextSlamBuffer = 0.0F;
   }

   protected void findNextPosition(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, "currentTarget");
      if (target != null) {
         Float base = new Float(target.x, target.y);
         Float pos = new Float(mob.x, mob.y);

         for (int i = 0; i < 10; i++) {
            int randomAngle = GameRandom.globalRandom.nextInt(360);
            Float angleDir = GameMath.getAngleDir(randomAngle);
            pos = new Float(base.x + angleDir.x * this.randomMoveRangeAroundTarget, base.y + angleDir.y * this.randomMoveRangeAroundTarget);
         }

         blackboard.mover.directMoveTo(this, (int)pos.x, (int)pos.y);
      }
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
