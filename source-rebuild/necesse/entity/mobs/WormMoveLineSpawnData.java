package necesse.entity.mobs;

import java.awt.geom.Point2D.Float;

public class WormMoveLineSpawnData {
   public Float lastPoint;
   public float movedDist = 0.0F;

   public WormMoveLineSpawnData(float startX, float startY) {
      this.lastPoint = new Float(startX, startY);
   }
}
