package necesse.entity.mobs.ai.path;

import java.awt.Point;
import java.util.ArrayList;
import java.util.stream.Stream;
import necesse.entity.mobs.Mob;
import necesse.gfx.camera.GameCamera;

public class FinalPath {
   private static float TILE_LENGTH = 32.0F;
   private static float TILE_DIAGONAL_LENGTH = (float)Math.sqrt(TILE_LENGTH * TILE_LENGTH * 2.0F);
   private ArrayList<FinalPathPoint> pathArray;
   private float removedPathLength;
   private float pathLength = -1.0F;

   public FinalPath(ArrayList<FinalPathPoint> pathArray) {
      this.pathArray = pathArray;
   }

   public float getCurrentLength() {
      if (this.pathLength == -1.0F) {
         this.pathLength = calculatePathLength(this.pathArray);
      }

      return this.pathLength;
   }

   public float getFullLength() {
      return this.getCurrentLength() + this.removedPathLength;
   }

   public int size() {
      return this.pathArray != null ? this.pathArray.size() : 0;
   }

   public void removeFirst() {
      FinalPathPoint last = this.pathArray.get(0);
      this.pathArray.remove(0);
      if (this.pathArray.size() <= 1) {
         this.pathLength = 0.0F;
      } else {
         FinalPathPoint next = this.pathArray.get(0);
         float pointLength = calculatePathLength(last, next);
         if (this.pathLength > 0.0F) {
            this.pathLength -= pointLength;
         }

         this.removedPathLength += pointLength;
      }
   }

   public FinalPathPoint getFirst() {
      return this.pathArray != null && this.pathArray.size() != 0 ? this.pathArray.get(0) : null;
   }

   public FinalPathPoint getLast() {
      return this.pathArray != null && this.pathArray.size() != 0 ? this.pathArray.get(this.pathArray.size() - 1) : null;
   }

   public Stream<FinalPathPoint> streamPathPoints() {
      return this.pathArray.stream();
   }

   public void drawPath(Mob mob, GameCamera camera) {
      TilePathfinding.drawPath(mob, this.pathArray, camera);
   }

   public static float calculatePathLength(ArrayList<? extends Point> pathArray) {
      if (pathArray != null && pathArray.size() != 1) {
         float lengthCounter = 0.0F;

         for (int i = 1; i < pathArray.size(); i++) {
            Point prev = pathArray.get(i - 1);
            Point current = pathArray.get(i);
            lengthCounter += calculatePathLength(prev, current);
         }

         return lengthCounter;
      } else {
         return 0.0F;
      }
   }

   public static float calculatePathLength(Point p1, Point p2) {
      int xDif = Math.abs(p1.x - p2.x);
      int yDif = Math.abs(p1.y - p2.y);
      if (xDif == 0) {
         return TILE_LENGTH * yDif;
      }

      if (xDif == yDif) {
         return TILE_DIAGONAL_LENGTH * xDif;
      }

      float counter = 0.0F;
      if (xDif < yDif) {
         while (yDif != xDif) {
            counter += TILE_LENGTH;
            yDif--;
         }

         counter += TILE_DIAGONAL_LENGTH * xDif;
      } else {
         while (xDif != yDif) {
            counter += TILE_LENGTH;
            xDif--;
         }

         counter += TILE_DIAGONAL_LENGTH * yDif;
      }

      return counter;
   }
}
