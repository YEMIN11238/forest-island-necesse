package necesse.engine.util;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import necesse.entity.mobs.Mob;

public class MovedRectangle extends Polygon {
   public MovedRectangle(Mob mob, int endX, int endY) {
      this(mob.getCollision(), endX - mob.getX(), endY - mob.getY());
   }

   public MovedRectangle(Mob mob, int startX, int startY, int endX, int endY) {
      this(mob.getCollision(startX, startY), endX - startX, endY - startY);
   }

   public MovedRectangle(Rectangle rect, int startX, int startY, int endX, int endY) {
      this(rect, endX - startX, endY - startY);
   }

   public MovedRectangle(Rectangle start, int deltaX, int deltaY) {
      Rectangle end = new Rectangle(start.x + deltaX, start.y + deltaY, start.width, start.height);
      if (deltaX > 0) {
         if (deltaY > 0) {
            this.build(botLeft(start), topLeft(start), topRight(start), topRight(end), botRight(end), botLeft(end));
         } else if (deltaY < 0) {
            this.build(botRight(start), botLeft(start), topLeft(start), topLeft(end), topRight(end), botRight(end));
         } else {
            this.build(botLeft(start), topLeft(start), topRight(end), botRight(end));
         }
      } else if (deltaX < 0) {
         if (deltaY > 0) {
            this.build(topLeft(start), topRight(start), botRight(start), botRight(end), botLeft(end), topLeft(end));
         } else if (deltaY < 0) {
            this.build(topRight(start), botRight(start), botLeft(start), botLeft(end), topLeft(end), topRight(end));
         } else {
            this.build(topRight(start), botRight(start), botLeft(end), topLeft(end));
         }
      } else if (deltaY > 0) {
         this.build(topLeft(start), topRight(start), botRight(end), botLeft(end));
      } else if (deltaY < 0) {
         this.build(botLeft(start), botRight(start), topRight(end), topLeft(end));
      } else {
         this.build(topLeft(start), topRight(start), botRight(start), botLeft(start));
      }
   }

   private void build(Point... points) {
      this.xpoints = new int[points.length];
      this.ypoints = new int[points.length];
      this.npoints = points.length;

      for (int i = 0; i < points.length; i++) {
         this.xpoints[i] = points[i].x;
         this.ypoints[i] = points[i].y;
      }
   }

   private static Point topLeft(Rectangle rectangle) {
      return new Point(rectangle.x, rectangle.y);
   }

   private static Point topRight(Rectangle rectangle) {
      return new Point(rectangle.x + rectangle.width, rectangle.y);
   }

   private static Point botLeft(Rectangle rectangle) {
      return new Point(rectangle.x, rectangle.y + rectangle.height);
   }

   private static Point botRight(Rectangle rectangle) {
      return new Point(rectangle.x + rectangle.width, rectangle.y + rectangle.height);
   }
}
