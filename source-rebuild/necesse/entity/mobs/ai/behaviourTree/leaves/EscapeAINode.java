package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.Comparator;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

public abstract class EscapeAINode<T extends Mob> extends MoveTaskAINode<T> {
   public long nextPathFind;
   public Float escapeDirection;

   public abstract boolean shouldEscape(T var1, Blackboard<T> var2);

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onEscaped(T mob) {
      mob.remove();
   }

   public static boolean canEscape(Mob mob, int edgePadding) {
      Level level = mob.getLevel();
      int mobTileX = mob.getTileX();
      int mobTileY = mob.getTileY();
      if (level.tileWidth > 0) {
         return mobTileX < edgePadding || mobTileX >= level.tileWidth - edgePadding - 1;
      } else if (level.tileHeight > 0) {
         return mobTileY < edgePadding || mobTileY >= level.tileHeight - edgePadding - 1;
      } else {
         Region region = level.regionManager.getRegionByTile(mobTileX, mobTileY, false);
         if (region == null) {
            return true;
         } else {
            int tileXWithinRegion = mobTileX - region.tileXOffset;
            int tileYWithinRegion = mobTileY - region.tileYOffset;
            if (tileXWithinRegion <= edgePadding && !isRegionLoaded(level, region.regionX - 1, region.regionY)) {
               return true;
            } else if (tileXWithinRegion >= region.tileWidth - 1 - edgePadding && !isRegionLoaded(level, region.regionX + 1, region.regionY)) {
               return true;
            } else {
               return tileYWithinRegion <= edgePadding && !isRegionLoaded(level, region.regionX, region.regionY - 1)
                  ? true
                  : tileYWithinRegion >= region.tileHeight - 1 - edgePadding && !isRegionLoaded(level, region.regionX, region.regionY + 1);
            }
         }
      }
   }

   private static boolean isRegionLoaded(Level level, int regionX, int regionY) {
      return level.regionManager.isRegionLoaded(regionX, regionY);
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (!this.shouldEscape(mob, blackboard)) {
         return AINodeResult.FAILURE;
      }

      if (canEscape(mob, 4)) {
         this.onEscaped(mob);
         return AINodeResult.SUCCESS;
      }

      int mobTileX = mob.getTileX();
      int mobTileY = mob.getTileY();
      if (this.escapeDirection == null) {
         ServerClient closest = GameUtils.streamServerClients(mob.getLevel()).min(Comparator.comparing(c -> mob.getDistance(c.playerMob))).orElse(null);
         if (closest != null) {
            this.escapeDirection = GameMath.normalize(mob.x - closest.playerMob.x, mob.y - closest.playerMob.y);
         }

         if (this.escapeDirection == null) {
            this.escapeDirection = GameMath.getAngleDir(GameRandom.globalRandom.nextInt(360));
         }
      }

      if (this.nextPathFind < mob.getLocalTime() || !blackboard.mover.isCurrentlyMovingFor(this)) {
         this.nextPathFind = mob.getLocalTime() + 1000L;

         for (int i = 32; i > 1; i--) {
            Point tile = new Point((int)(mobTileX + this.escapeDirection.x * i), (int)(mobTileY + this.escapeDirection.y * i));
            if (mob.estimateCanMoveTo(tile.x, tile.y, true)) {
               return this.moveToTileTask(tile.x, tile.y, null, path -> {
                  if (path.moveIfWithin(-1, -1, () -> this.nextPathFind = 0L)) {
                     int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, 2000, 0.1F);
                     this.nextPathFind = mob.getLocalTime() + nextPathTimeAdd;
                  }

                  return AINodeResult.SUCCESS;
               });
            }
         }

         float escapeAngle = GameMath.getAngle(this.escapeDirection);
         this.escapeDirection = GameMath.getAngleDir(GameRandom.globalRandom.getFloatOffset(escapeAngle, 90.0F));
      }

      return AINodeResult.SUCCESS;
   }
}
