package necesse.entity.mobs.summon;

import necesse.engine.util.GameMath;

public class MinecartLines {
   public int tileX;
   public int tileY;
   public MinecartLine up;
   public MinecartLine right;
   public MinecartLine down;
   public MinecartLine left;

   public MinecartLines(int tileX, int tileY) {
      this.tileX = tileX;
      this.tileY = tileY;
   }

   public MinecartLinePos getMinecartPos(float entityX, float entityY, int entityDir) {
      if (entityDir == 0) {
         return this.bestPos(entityX, entityY, entityDir, this.up, this.right, this.left, this.down);
      } else if (entityDir == 1) {
         return this.bestPos(entityX, entityY, entityDir, this.right, this.up, this.down, this.left);
      } else {
         return entityDir == 2
            ? this.bestPos(entityX, entityY, entityDir, this.down, this.right, this.left, this.up)
            : this.bestPos(entityX, entityY, entityDir, this.left, this.up, this.down, this.right);
      }
   }

   private MinecartLinePos bestPos(float entityX, float entityY, int entityDir, MinecartLine... linePriority) {
      MinecartLinePos best = null;
      float bestDist = 0.0F;

      for (MinecartLine line : linePriority) {
         if (line != null) {
            MinecartLinePos next = this.linePos(line, entityX, entityY, entityDir);
            float nextDist = GameMath.diamondDistance(next.x, next.y, entityX, entityY);
            if (best == null || nextDist < bestDist) {
               best = next;
               bestDist = nextDist;
            }
         }
      }

      return best;
   }

   private MinecartLinePos linePos(MinecartLine line, float entityX, float entityY, int entityDir) {
      if (line.dir != 1 && line.dir != 3) {
         int dir = entityDir != 1 && entityDir != 0 ? 2 : 0;
         float distanceAlong;
         if (entityY <= line.y1) {
            distanceAlong = 0.0F;
         } else if (entityY >= line.y2) {
            distanceAlong = line.distance;
         } else {
            distanceAlong = entityY - line.y1;
         }

         if (entityDir == 0 || entityDir == 2) {
            if (line.dir == 2 && distanceAlong >= line.distance) {
               dir = line.dir;
            } else if (line.dir == 0 && distanceAlong <= 0.0F) {
               dir = line.dir;
            }
         }

         return new MinecartLinePos(line, GameMath.limit(entityX, line.x1, line.x2), GameMath.limit(entityY, line.y1, line.y2), distanceAlong, dir);
      } else {
         int dir = entityDir != 1 && entityDir != 0 ? 3 : 1;
         float distanceAlong;
         if (entityX <= line.x1) {
            distanceAlong = 0.0F;
         } else if (entityX >= line.x2) {
            distanceAlong = line.distance;
         } else {
            distanceAlong = entityX - line.x1;
         }

         if (entityDir == 0 || entityDir == 2) {
            if (line.dir == 3 && distanceAlong >= line.distance) {
               dir = line.dir;
            } else if (line.dir == 1 && distanceAlong <= 0.0F) {
               dir = line.dir;
            }
         }

         return new MinecartLinePos(line, GameMath.limit(entityX, line.x1, line.x2), GameMath.limit(entityY, line.y1, line.y2), distanceAlong, dir);
      }
   }
}
