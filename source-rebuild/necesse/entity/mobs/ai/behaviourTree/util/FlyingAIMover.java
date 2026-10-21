package necesse.entity.mobs.ai.behaviourTree.util;

import java.awt.Point;
import java.util.function.BiPredicate;
import java.util.function.Function;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;

public class FlyingAIMover extends AIMover {
   @Override
   public MoveToTileAITask moveToTileTask(
      AINode<?> node,
      int tileX,
      int tileY,
      BiPredicate<Point, Point> isAtTarget,
      int maxPathIterations,
      Function<MoveToTileAITask.AIPathResult, AINodeResult> handler
   ) {
      MoveToTileAITask task = MoveToTileAITask.directMoveToTile(this, node, tileX, tileY, handler);
      task.runNow();
      return task;
   }
}
