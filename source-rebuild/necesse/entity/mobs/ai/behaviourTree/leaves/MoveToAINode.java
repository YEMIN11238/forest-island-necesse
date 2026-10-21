package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.util.function.BiPredicate;
import necesse.engine.GameLog;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.event.SetMoveToTileAIEvent;
import necesse.entity.mobs.ai.path.TilePathfinding;
import necesse.entity.mobs.friendly.human.MoveToTile;

public class MoveToAINode<T extends Mob> extends MoveTaskAINode<T> {
   public MoveToTile moveToTile;
   public boolean clearWhenArrived;
   public long pathFindingCooldown;
   private final String setEventType;

   public MoveToAINode(String setEventType) {
      this.setEventType = setEventType;
   }

   public MoveToAINode() {
      this("setMoveToTile");
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      if (this.setEventType != null) {
         blackboard.onEvent(this.setEventType, e -> {
            if (e instanceof SetMoveToTileAIEvent) {
               SetMoveToTileAIEvent event = (SetMoveToTileAIEvent)e;
               this.moveToTile = event.moveToTile;
               this.clearWhenArrived = event.clearWhenArrived;
            } else {
               GameLog.warn.println("MoveToAINode got invalid event: " + e + ", Mob: " + mob);
            }
         });
      }
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   public MoveToTile getMoveToTile(T mob, Blackboard<T> blackboard) {
      return this.moveToTile;
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      MoveToTile moveToTile = this.getMoveToTile(mob, blackboard);
      if (moveToTile != null) {
         float tileDist = (float)moveToTile.distance(mob.getTileX(), mob.getTileY());
         if (this.pathFindingCooldown < mob.getTime()) {
            this.pathFindingCooldown = mob.getTime() + 2000L;
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
                  this.pathFindingCooldown = mob.getTime() + nextPathTimeAdd;
               }

               if (moveToTile.isAtLocation(tileDist, foundPathTo)) {
                  moveToTile.onArrivedAtLocation();
                  if (this.clearWhenArrived) {
                     this.moveToTile = null;
                  }
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
                  if (this.clearWhenArrived) {
                     this.moveToTile = null;
                  }
               }
            }

            return AINodeResult.SUCCESS;
         }
      } else {
         return AINodeResult.FAILURE;
      }
   }
}
