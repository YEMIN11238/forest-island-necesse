package necesse.entity.mobs.ai.path;

import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.MovedRectangle;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PathDoorOption;
import necesse.level.gameObject.DoorObject;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;

public class PathOptions {
   public final TilePathfinding.NodePriority nodePriority;

   public PathOptions(TilePathfinding.NodePriority nodePriority) {
      this.nodePriority = nodePriority;
   }

   public PathOptions() {
      this(TilePathfinding.NodePriority.TOTAL_COST);
   }

   public boolean canPassTile(
      TickManager tickManager, Level level, Mob mob, PathDoorOption doorOption, CollisionFilter levelCollisionFilter, int tileX, int tileY
   ) {
      return doorOption.canPass(tileX, tileY);
   }

   public boolean checkCanPassDoorOrTile(
      TickManager tickManager, Level level, Mob mob, PathDoorOption doorOption, CollisionFilter levelCollisionFilter, int tileX, int tileY
   ) {
      GameObject object = level.getObject(tileX, tileY);
      return object.isDoor && doorOption.canPassDoor((DoorObject)object, tileX, tileY)
         ? true
         : doorOption.canBreakDown(tileX, tileY) || this.canPassTile(tickManager, level, mob, doorOption, levelCollisionFilter, tileX, tileY);
   }

   public double getTileCost(Level level, Mob mob, PathDoorOption doorOption, int tileX, int tileY) {
      double mod = level.getTile(tileX, tileY).getPathCost(level, tileX, tileY, mob);
      GameObject object = level.getObject(tileX, tileY);
      mod += object.getPathCost(level, tileX, tileY);
      if (doorOption == null) {
         return mod;
      }

      if (!object.isDoor
         || !((DoorObject)object).isOpen(level, tileX, tileY, level.getObjectRotation(tileX, tileY))
            && (!doorOption.canOpen(tileX, tileY) || ((DoorObject)object).isForceClosed(level, tileX, tileY))) {
         if (object.isSolid(level, tileX, tileY) && doorOption.canBreakDown(tileX, tileY)) {
            if (mob != null) {
               double cost = mob.getPathBreakDownCost(level, tileX, tileY, object);
               if (cost >= 0.0) {
                  mod += cost;
               } else {
                  mod += object.getBreakDownPathCost(level, tileX);
               }
            } else {
               mod += object.getBreakDownPathCost(level, tileX);
            }
         }

         return mod;
      } else {
         return mod;
      }
   }

   public boolean canMoveLine(TickManager tickManager, Level level, Mob mob, CollisionFilter levelCollisionFilter, int x1, int y1, int x2, int y2) {
      return Performance.record(tickManager, "canPassLine", () -> !level.collides(new MovedRectangle(mob, x1, y1, x2, y2), levelCollisionFilter));
   }
}
