package necesse.entity.mobs.hostile.bosses.bossAIUtils;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;

public class MoveToRandomTileAttackStage<T extends Mob> extends MoveTaskAINode<T> implements AttackStageInterface<T> {
   public int baseDistance;
   public boolean isRunningWhileMoving;
   public String targetKey;

   public MoveToRandomTileAttackStage(boolean isRunningWhileMoving, int baseDistance, String targetKey) {
      this.isRunningWhileMoving = isRunningWhileMoving;
      this.baseDistance = baseDistance;
      this.targetKey = targetKey;
   }

   public MoveToRandomTileAttackStage(boolean isRunningWhileMoving, int baseDistance) {
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

      Point tileOffset = mob.getPathMoveOffset();
      Point tile = new Point(mob.getTileX(), mob.getTileY());

      for (int i = 0; i < 20; i++) {
         int randomAngle = GameRandom.globalRandom.nextInt(360);
         Float angleDir = GameMath.getAngleDir(randomAngle);
         tile = new Point(
            GameMath.getTileCoordinate(base.x + angleDir.x * this.baseDistance), GameMath.getTileCoordinate(base.y + angleDir.y * this.baseDistance)
         );
         if (!mob.collidesWith(mob.getLevel(), tile.x * 32 + tileOffset.x, tile.y * 32 + tileOffset.y)
            && mob.getDistance(tile.x * 32 + tileOffset.x, tile.y * 32 + tileOffset.y) >= this.baseDistance / 4.0F
            && mob.estimateCanMoveTo(tile.x, tile.y, false)) {
            break;
         }
      }

      this.moveToTileTask(tile.x, tile.y, null, path -> {
         if (path.moveIfWithin(-1, -1, null)) {
            return this.isRunningWhileMoving ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
         } else {
            return AINodeResult.FAILURE;
         }
      });
   }

   @Override
   public void onEnded(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      return this.isRunningWhileMoving && blackboard.mover.isMoving() ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
   }
}
