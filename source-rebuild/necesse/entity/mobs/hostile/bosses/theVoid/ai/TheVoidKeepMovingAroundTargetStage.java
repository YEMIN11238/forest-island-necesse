package necesse.entity.mobs.hostile.bosses.theVoid.ai;

import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.TheVoidMovingRainLevelEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;

public abstract class TheVoidKeepMovingAroundTargetStage<T extends TheVoidMob> extends AINode<T> implements AttackStageInterface<T> {
   public float keepRunningAndFindNewPositionsAtDistance;
   protected TheVoidMovingRainLevelEvent event;

   public TheVoidKeepMovingAroundTargetStage(float keepRunningAndFindNewPositionsAtDistance) {
      this.keepRunningAndFindNewPositionsAtDistance = keepRunningAndFindNewPositionsAtDistance;
   }

   public void tickFindNextPosition(T mob, Blackboard<T> blackboard) {
      if (this.keepRunningAndFindNewPositionsAtDistance > 0.0F && !blackboard.mover.isCurrentlyMovingFor(this)) {
         this.findNextPosition(mob, blackboard);
      }
   }

   protected void findNextPosition(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, "currentTarget");
      if (target != null) {
         Float base = new Float(target.x, target.y);
         Float pos = new Float(mob.x, mob.y);

         for (int i = 0; i < 10; i++) {
            int randomAngle = GameRandom.globalRandom.nextInt(360);
            Float angleDir = GameMath.getAngleDir(randomAngle);
            pos = new Float(
               base.x + angleDir.x * this.keepRunningAndFindNewPositionsAtDistance, base.y + angleDir.y * this.keepRunningAndFindNewPositionsAtDistance
            );
         }

         blackboard.mover.directMoveTo(this, (int)pos.x, (int)pos.y);
      }
   }
}
