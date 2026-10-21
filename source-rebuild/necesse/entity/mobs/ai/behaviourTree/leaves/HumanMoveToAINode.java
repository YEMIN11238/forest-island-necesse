package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.util.function.BiPredicate;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.path.TilePathfinding;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.MoveToTile;

public class HumanMoveToAINode<T extends HumanMob> extends MoveTaskAINode<T> {
   public long pathFindingCooldown;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      blackboard.onEvent("newTargetFound", e -> {
         if (mob.objectUser != null) {
            mob.objectUser.stopUsing();
         }
      });
      blackboard.onEvent("resetPathTime", e -> this.pathFindingCooldown = 0L);
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      MoveToTile moveToTile = mob.getMoveToPoint();
      if (moveToTile != null) {
         if (mob.objectUser != null) {
            mob.objectUser.stopUsing();
         }

         float tileDist = (float)moveToTile.distance(GameMath.getTileFloatCoordinate(mob.getX()), GameMath.getTileFloatCoordinate(mob.getY()));
         if (this.pathFindingCooldown < mob.getLocalTime()) {
            this.pathFindingCooldown = mob.getLocalTime() + 2000L;
            BiPredicate<Point, Point> isAtTarget;
            if (moveToTile.acceptAdjacentTiles) {
               isAtTarget = TilePathfinding.isAtOrAdjacentObject(mob.getLevel(), moveToTile.x, moveToTile.y);
            } else {
               isAtTarget = null;
            }

            return this.moveToTileTask(moveToTile.x, moveToTile.y, isAtTarget, path -> {
               boolean foundPathTo;
               if (moveToTile.moveIfPathFailed(tileDist)) {
                  foundPathTo = path.moveIfWithin(-1, -1, () -> this.pathFindingCooldown = 0L);
               } else {
                  foundPathTo = path.moveIfWithin(-1, 1, () -> this.pathFindingCooldown = 0L);
               }

               if (foundPathTo) {
                  int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, 2000, 0.1F);
                  this.pathFindingCooldown = mob.getLocalTime() + nextPathTimeAdd;
               }

               if (moveToTile.isAtLocation(tileDist, foundPathTo)) {
                  moveToTile.onArrivedAtLocation();
               }

               return AINodeResult.SUCCESS;
            });
         } else {
            Point finalDestination = blackboard.mover.getFinalDestination();
            if (finalDestination != null) {
               BiPredicate<Point, Point> isAtTarget;
               if (moveToTile.acceptAdjacentTiles) {
                  isAtTarget = TilePathfinding.isAtOrAdjacentObject(mob.getLevel(), moveToTile.x, moveToTile.y);
               } else {
                  isAtTarget = (p1, p2) -> p1.x == p2.x && p1.y == p2.y;
               }

               if (moveToTile.isAtLocation(tileDist, isAtTarget.test(finalDestination, moveToTile))) {
                  moveToTile.onArrivedAtLocation();
               }
            }

            return AINodeResult.SUCCESS;
         }
      } else {
         return AINodeResult.FAILURE;
      }
   }
}
