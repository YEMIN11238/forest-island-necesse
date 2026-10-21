package necesse.level.gameObject.furniture;

import java.awt.Color;
import java.awt.Point;
import necesse.level.maps.Level;

public class SpiderCastleChairObject extends ChairObject {
   public SpiderCastleChairObject(String textureName, Color mapColor) {
      super(textureName, mapColor);
      this.alwaysRenderBehindUser = true;
   }

   @Override
   public Point getMobPosSitOffset(Level level, int tileX, int tileY) {
      int rotation = level.getObjectRotation(tileX, tileY);
      switch (rotation) {
         case 0:
            return new Point(16, 12);
         case 1:
            return new Point(16, 15);
         case 2:
            return new Point(16, 16);
         case 3:
            return new Point(16, 15);
         default:
            return new Point(16, 16);
      }
   }
}
