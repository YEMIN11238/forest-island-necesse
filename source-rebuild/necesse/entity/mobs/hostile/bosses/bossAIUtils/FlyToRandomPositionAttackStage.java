package necesse.entity.mobs.hostile.bosses.bossAIUtils;

import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;

public class FlyToRandomPositionAttackStage<T extends Mob> extends AINode<T> implements AttackStageInterface<T> {
   public int baseDistance;
   public boolean isRunningWhileMoving;
   public String targetKey;

   public FlyToRandomPositionAttackStage(boolean isRunningWhileMoving, int baseDistance, String targetKey) {
      this.isRunningWhileMoving = isRunningWhileMoving;
      this.baseDistance = baseDistance;
      this.targetKey = targetKey;
   }

   public FlyToRandomPositionAttackStage(boolean isRunningWhileMoving, int baseDistance) {
      this(isRunningWhileMoving, baseDistance, "currentTarget");
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void onStarted(T mob, Blackboard<T> blackboard) {
      Mob currentTarget = blackboard.getObject(Mob.class, this.targetKey);
      Float base = new Float(mob.x, mob.y);
      if (currentTarget != null) {
         base = new Float(currentTarget.x, currentTarget.y);
      }

      Float pos = new Float(mob.x, mob.y);

      for (int i = 0; i < 10; i++) {
         int randomAngle = GameRandom.globalRandom.nextInt(360);
         Float angleDir = GameMath.getAngleDir(randomAngle);
         pos = new Float(base.x + angleDir.x * this.baseDistance, base.y + angleDir.y * this.baseDistance);
         if (mob.getDistance(pos.x, pos.y) >= this.baseDistance / 4.0F) {
            break;
         }
      }

      blackboard.mover.directMoveTo(this, (int)pos.x, (int)pos.y);
   }

   @Override
   public void onEnded(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return this.isRunningWhileMoving && blackboard.mover.isMoving() ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
   }
}
