package necesse.entity.mobs.hostile.bosses.bossAIUtils;

import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;

public class FlyToOppositeDirectionAttackStage<T extends Mob> extends AINode<T> implements AttackStageInterface<T> {
   public boolean isRunningWhileMoving;
   public float distance;
   public float randomAngleMaxOffset;
   public String targetKey;

   public FlyToOppositeDirectionAttackStage(boolean isRunningWhileMoving, float distance, float randomAngleMaxOffset, String targetKey) {
      this.isRunningWhileMoving = isRunningWhileMoving;
      this.distance = distance;
      this.randomAngleMaxOffset = randomAngleMaxOffset;
      this.targetKey = targetKey;
   }

   public FlyToOppositeDirectionAttackStage(boolean isRunningWhileMoving, float distance, float randomAngleMaxOffset) {
      this(isRunningWhileMoving, distance, randomAngleMaxOffset, "currentTarget");
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void onStarted(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, this.targetKey);
      if (target != null) {
         Float dir = GameMath.normalize(target.x - mob.x, target.y - mob.y);
         float angle = GameMath.getAngle(dir) + GameRandom.globalRandom.getFloatBetween(-this.randomAngleMaxOffset, this.randomAngleMaxOffset);
         Float finalDir = GameMath.getAngleDir(angle);
         blackboard.mover.directMoveTo(this, (int)(target.x + finalDir.x * this.distance), (int)(target.y + finalDir.y * this.distance));
      }
   }

   @Override
   public void onEnded(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return this.isRunningWhileMoving && blackboard.mover.isMoving() ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
   }
}
