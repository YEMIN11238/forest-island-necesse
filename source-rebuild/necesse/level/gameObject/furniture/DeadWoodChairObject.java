package necesse.level.gameObject.furniture;

import java.awt.Color;
import java.awt.Point;
import necesse.level.maps.Level;

public class DeadWoodChairObject extends ChairObject {
   public DeadWoodChairObject(String textureName, Color mapColor, String... category) {
      super(textureName, mapColor, category);
   }

   @Override
   public Point getMobPosSitOffset(Level level, int tileX, int tileY) {
      int rotation = level.getObjectRotation(tileX, tileY);
      switch (rotation) {
         case 0:
            return new Point(16, 11);
         case 1:
            return new Point(20, 10);
         case 2:
            return new Point(16, 14);
         case 3:
            return new Point(12, 10);
         default:
            return new Point(16, 16);
      }
   }
}
