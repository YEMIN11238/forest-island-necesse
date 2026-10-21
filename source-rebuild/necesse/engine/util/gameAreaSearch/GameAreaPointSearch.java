package necesse.engine.util.gameAreaSearch;

import java.awt.Point;

public class GameAreaPointSearch extends GameAreaSearch<Point> {
   public GameAreaPointSearch(int startX, int startY, int minX, int minY, int maxX, int maxY, int maxDistance) {
      super(startX, startY, minX, minY, maxX, maxY, maxDistance);
   }

   public GameAreaPointSearch(int startX, int startY, int maxDistance) {
      super(startX, startY, maxDistance);
   }

   protected Point get(int x, int y) {
      return new Point(x, y);
   }
}
