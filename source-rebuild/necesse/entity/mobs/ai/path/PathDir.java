package necesse.entity.mobs.ai.path;

import java.awt.Point;

public enum PathDir {
   UP(0, -1),
   UP_RIGHT(1, -1),
   RIGHT(1, 0),
   DOWN_RIGHT(1, 1),
   DOWN(0, 1),
   DOWN_LEFT(-1, 1),
   LEFT(-1, 0),
   UP_LEFT(-1, -1);

   public final Point point;
   public final int x;
   public final int y;
   public final boolean isDiagonal;
   public final int dir;

   PathDir(int x, int y) {
      if (Math.abs(x) <= 1 && Math.abs(y) <= 1) {
         this.point = new Point(x, y);
         this.x = x;
         this.y = y;
         this.isDiagonal = x != 0 && y != 0;
         if (x == 0 && y < 0) {
            this.dir = 0;
         } else if (x > 0 && y < 0) {
            this.dir = 1;
         } else if (x > 0 && y == 0) {
            this.dir = 2;
         } else if (x > 0) {
            this.dir = 3;
         } else if (x == 0 && y > 0) {
            this.dir = 4;
         } else if (x < 0 && y > 0) {
            this.dir = 5;
         } else if (x < 0 && y == 0) {
            this.dir = 6;
         } else {
            this.dir = 7;
         }
      } else {
         throw new IllegalArgumentException("Cannot be offset more than 1");
      }
   }

   public static PathDir getDir(Point firstPoint, Point nextPoint) {
      int x = firstPoint.x - nextPoint.x;
      int y = firstPoint.y - nextPoint.y;
      if (x == 0 && y < 0) {
         return UP;
      } else if (x > 0 && y < 0) {
         return UP_RIGHT;
      } else if (x > 0 && y == 0) {
         return RIGHT;
      } else if (x > 0) {
         return DOWN_RIGHT;
      } else if (x == 0 && y > 0) {
         return DOWN;
      } else if (x < 0 && y > 0) {
         return DOWN_LEFT;
      } else if (x < 0 && y == 0) {
         return LEFT;
      } else {
         return x < 0 ? UP_LEFT : null;
      }
   }
}
