package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashSet;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.path.PathDir;
import necesse.entity.mobs.ai.path.TilePathfinding;
import necesse.entity.mobs.mobMovement.MobMovementLevelPos;
import necesse.level.maps.regionSystem.RegionType;
import necesse.level.maps.regionSystem.SubRegion;

public abstract class CommandMoveToAINode<T extends Mob> extends MoveTaskAINode<T> {
   public long nextPathFindTime;
   public long nextCheckArrivedTime;
   public boolean isDirectMovingTo;

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      blackboard.onEvent("newCommandSet", event -> {
         this.nextPathFindTime = 0L;
         this.nextCheckArrivedTime = 0L;
      });
      blackboard.onEvent("resetPathTime", e -> this.nextPathFindTime = 0L);
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (this.isDirectMovingTo && blackboard.mover.isCurrentlyMovingFor(this) && mob.hasArrivedAtTarget()) {
         this.nextCheckArrivedTime = mob.getLocalTime() + 1000L;
         this.onArrived(mob);
         blackboard.mover.stopMoving(mob);
         this.isDirectMovingTo = false;
         this.updateActivityDescription(mob, new LocalMessage("activities", "watchingfortargets"));
         this.tickStandingStill(mob);
         return AINodeResult.SUCCESS;
      }

      Point pos = this.getLevelPosition(mob);
      if (pos == null) {
         this.isDirectMovingTo = false;
         this.updateActivityDescription(mob, null);
         return AINodeResult.FAILURE;
      }

      if (this.nextCheckArrivedTime > mob.getLocalTime()) {
         this.updateActivityDescription(mob, new LocalMessage("activities", "watchingfortargets"));
         this.tickStandingStill(mob);
         return AINodeResult.SUCCESS;
      }

      int tileX = GameMath.getTileCoordinate(pos.x);
      int tileY = GameMath.getTileCoordinate(pos.y);
      if (isAtPosition(mob, pos.x, pos.y, new HashSet<>())) {
         this.nextCheckArrivedTime = mob.getLocalTime() + 1000L;
         this.onArrived(mob);
         blackboard.mover.stopMoving(mob);
         this.isDirectMovingTo = false;
         this.updateActivityDescription(mob, new LocalMessage("activities", "watchingfortargets"));
         this.tickStandingStill(mob);
         return AINodeResult.SUCCESS;
      }

      if (mob.getTileX() == tileX && mob.getTileY() == tileY) {
         if (!this.isDirectMovingTo) {
            blackboard.mover.setCustomMovement(this, new MobMovementLevelPos(pos.x, pos.y));
            this.isDirectMovingTo = true;
         }

         this.updateActivityDescription(mob, new LocalMessage("activities", "movingtoposition"));
         this.tickMoving(mob);
         return AINodeResult.SUCCESS;
      } else {
         this.isDirectMovingTo = false;
         if (this.nextPathFindTime <= mob.getLocalTime()) {
            if (mob.estimateCanMoveTo(tileX, tileY, false)) {
               return this.moveToTileTask(tileX, tileY, null, path -> {
                  if (path.moveIfWithin(-1, -1, () -> this.nextPathFindTime = 0L)) {
                     int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, 2000, 0.1F);
                     this.nextPathFindTime = mob.getLocalTime() + nextPathTimeAdd;
                  } else {
                     this.nextPathFindTime = mob.getLocalTime() + GameRandom.globalRandom.getIntBetween(10000, 16000);
                  }

                  this.updateActivityDescription(mob, new LocalMessage("activities", "movingtoposition"));
                  this.tickMoving(mob);
                  return AINodeResult.SUCCESS;
               });
            }

            this.nextCheckArrivedTime = mob.getLocalTime() + 4000L;
            this.onCannotMoveTo(mob, pos.x, pos.y);
            this.isDirectMovingTo = false;
            this.updateActivityDescription(mob, new LocalMessage("activities", "watchingfortargets"));
            return AINodeResult.SUCCESS;
         } else {
            if (!blackboard.mover.isMoving() || !blackboard.mover.isCurrentlyMovingFor(this)) {
               this.nextPathFindTime = 0L;
            }

            this.updateActivityDescription(mob, new LocalMessage("activities", "movingtoposition"));
            this.tickMoving(mob);
            return AINodeResult.SUCCESS;
         }
      }
   }

   public static boolean isAtPosition(Mob mob, int x, int y, HashSet<Integer> checked) {
      Rectangle collision = mob.getCollision();
      if (collision.contains(x, y)) {
         return true;
      }

      checked.add(mob.getUniqueID());
      Rectangle extendedCollision = new Rectangle(collision.x - 4, collision.y - 4, collision.width + 8, collision.height + 8);
      int range = (int)GameMath.max(GameMath.diagonalMoveDistance(0, 0, extendedCollision.width, extendedCollision.height), 100.0);
      return mob.getLevel()
         .entityManager
         .mobs
         .streamInRegionsInRange(mob.x, mob.y, range)
         .filter(m -> m != mob && !checked.contains(m.getUniqueID()) && extendedCollision.intersects(m.getCollision()))
         .sequential()
         .anyMatch(m -> {
            checked.add(m.getUniqueID());
            return isAtPosition(m, x, y, checked);
         });
   }

   public static Point findClosestMoveToTile(Mob mob, int targetX, int targetY) {
      int targetTileX = GameMath.getTileCoordinate(targetX);
      int targetTileY = GameMath.getTileCoordinate(targetY);
      if (mob.estimateCanMoveTo(targetTileX, targetTileY, false)) {
         return new Point(targetTileX, targetTileY);
      }

      SubRegion subregion = mob.getLevel().regionManager.getSubRegionByTile(targetTileX, targetTileY);
      return subregion == null ? null : findClosestMoveToTile(mob, targetX, targetY, new HashSet<>(Collections.singleton(subregion)), new HashSet<>());
   }

   private static Point findClosestMoveToTile(Mob mob, int targetX, int targetY, HashSet<SubRegion> subregions, HashSet<SubRegion> checkRegionTiles) {
      for (SubRegion subregion : new HashSet<>(subregions)) {
         subregion.addAllConnected(subregions, srx -> srx.getType() != RegionType.OPEN, 1000);
      }

      Point bestTile = null;
      double bestDist = 0.0;

      for (SubRegion sr : subregions) {
         if (!checkRegionTiles.contains(sr)) {
            for (Point tile : sr.getLevelTiles()) {
               for (PathDir offset : TilePathfinding.nonDiagonalPoints) {
                  int tileX = tile.x + offset.x;
                  int tileY = tile.y + offset.y;
                  SubRegion subregion = mob.getLevel().regionManager.getSubRegionByTile(tileX, tileY);
                  if (!subregions.contains(subregion) && mob.estimateCanMoveTo(tileX, tileY, false)) {
                     if (bestTile == null) {
                        bestTile = new Point(tileX, tileY);
                        bestDist = GameMath.diagonalMoveDistance(tileX * 32 + 16, tileY * 32 + 16, targetX, targetY);
                     } else {
                        double dist = GameMath.diagonalMoveDistance(tileX * 32 + 16, tileY * 32 + 16, targetX, targetY);
                        if (dist < bestDist) {
                           bestTile = new Point(tileX, tileY);
                           bestDist = dist;
                        }
                     }
                  }
               }
            }

            checkRegionTiles.add(sr);
         }
      }

      if (bestTile == null) {
         boolean addedSome = false;

         for (SubRegion subregion : new HashSet<>(subregions)) {
            if (subregion.addAllConnected(subregions, srx -> srx.getType() == RegionType.OPEN, 1000) > subregion.size()) {
               addedSome = true;
            }
         }

         return !addedSome ? null : findClosestMoveToTile(mob, targetX, targetY, subregions, checkRegionTiles);
      } else {
         return bestTile;
      }
   }

   public abstract Point getLevelPosition(T var1);

   public abstract void onArrived(T var1);

   public void tickMoving(T mob) {
   }

   public void tickStandingStill(T mob) {
   }

   public abstract void onCannotMoveTo(T var1, int var2, int var3);

   public abstract void updateActivityDescription(T var1, GameMessage var2);
}
