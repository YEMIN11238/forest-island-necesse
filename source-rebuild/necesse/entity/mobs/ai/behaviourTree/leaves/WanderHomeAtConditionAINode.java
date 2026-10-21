package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.level.maps.regionSystem.ConnectedSubRegionsResult;

public abstract class WanderHomeAtConditionAINode<T extends Mob> extends MoveTaskAINode<T> {
   public long nextPathFindTime;

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      blackboard.onEvent("resetPathTime", e -> this.nextPathFindTime = 0L);
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (this.shouldGoHome(mob)) {
         if (this.nextPathFindTime <= mob.getWorldEntity().getLocalTime()) {
            Point home = this.getHomeTile(mob);
            if (home != null) {
               boolean findNewPos = this.shouldFindNewPos(mob, home);
               if (findNewPos) {
                  Point newPos = null;
                  if (mob.getLevel().isOutside(home.x, home.y)) {
                     int outsideHomeRadius = this.getOutsideHomeRadius(mob);
                     newPos = WandererAINode.findWanderingPointAround(
                        mob,
                        home.x,
                        home.y,
                        outsideHomeRadius,
                        null,
                        (tp, biome) -> mob.getDistance(tp.tileX * 32 + 16, tp.tileY * 32 + 16) > outsideHomeRadius * 32
                           ? Integer.MIN_VALUE
                           : mob.getTileWanderPriority(tp, biome),
                        20,
                        5
                     );
                  } else {
                     ConnectedSubRegionsResult regions = null;
                     if (this.isHomeRoom(mob)) {
                        regions = mob.getLevel().regionManager.getRoomConnectedByTile(home.x, home.y, true, 2000);
                     } else if (this.isHomeHouse(mob)) {
                        regions = mob.getLevel().regionManager.getHouseConnectedByTile(home.x, home.y, 4000);
                     }

                     if (regions != null) {
                        newPos = WandererAINode.findWanderingPointInsideRegions(mob, regions, 25, mob::getTileWanderPriority, 20, 5);
                     }
                  }

                  if (newPos != null) {
                     return this.moveToTileTask(newPos.x, newPos.y, null, path -> {
                        if (path.moveIfWithin(-1, -1, () -> this.nextPathFindTime = 0L)) {
                           int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, 2000, 0.1F);
                           this.nextPathFindTime = mob.getWorldEntity().getLocalTime() + nextPathTimeAdd;
                        } else {
                           this.nextPathFindTime = mob.getWorldEntity().getLocalTime() + GameRandom.globalRandom.getIntBetween(10000, 16000);
                        }

                        return AINodeResult.RUNNING;
                     });
                  }

                  this.nextPathFindTime = mob.getWorldEntity().getLocalTime() + GameRandom.globalRandom.getIntBetween(10000, 16000);
               }
            }
         }

         if (blackboard.mover.isMoving() && blackboard.mover.isCurrentlyMovingFor(this)) {
            Point base = this.getHomeTile(mob);
            return base != null && !this.shouldFindNewPos(mob, base) ? AINodeResult.FAILURE : AINodeResult.RUNNING;
         } else {
            return AINodeResult.FAILURE;
         }
      } else {
         this.nextPathFindTime = 0L;
         return AINodeResult.FAILURE;
      }
   }

   protected int getOutsideHomeRadius(T mob) {
      return 6;
   }

   protected boolean shouldFindNewPos(T mob, Point homeTile) {
      boolean isOutside = mob.getLevel().isOutside(homeTile.x, homeTile.y);
      return isOutside
         ? mob.getDistance(homeTile.x * 32 + 16, homeTile.y * 32 + 16) > this.getOutsideHomeRadius(mob) * 32
         : mob.getLevel().getRoomID(mob.getTileX(), mob.getTileY()) != mob.getLevel().getRoomID(homeTile.x, homeTile.y);
   }

   public abstract boolean shouldGoHome(T var1);

   public abstract Point getHomeTile(T var1);

   public abstract boolean isHomeRoom(T var1);

   public abstract boolean isHomeHouse(T var1);
}
