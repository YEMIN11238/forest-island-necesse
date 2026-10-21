package necesse.engine;

import java.awt.Point;
import java.awt.geom.Point2D.Float;

public class GameBezierPoint {
   public float startX;
   public float startY;
   public float targetX;
   public float targetY;

   public GameBezierPoint(float startX, float startY, float targetX, float targetY) {
      this.startX = startX;
      this.startY = startY;
      this.targetX = targetX;
      this.targetY = targetY;
   }

   public GameBezierPoint(Float startPos, Float targetPos) {
      this(startPos.x, startPos.y, targetPos.x, targetPos.y);
   }

   public GameBezierPoint(Point startPos, Point targetPos) {
      this(startPos.x, startPos.y, targetPos.x, targetPos.y);
   }

   public float getPointXOnCurve(GameBezierPoint targetPoint, float progress) {
      return (float)(
         Math.pow(1.0F - progress, 3.0) * this.startX
            + 3.0F * progress * Math.pow(1.0F - progress, 2.0) * this.targetX
            + 3.0 * Math.pow(progress, 2.0) * (1.0F - progress) * targetPoint.targetX
            + Math.pow(progress, 3.0) * targetPoint.startX
      );
   }

   public float getPointYOnCurve(GameBezierPoint targetPoint, float progress) {
      return (float)(
         Math.pow(1.0F - progress, 3.0) * this.startY
            + 3.0F * progress * Math.pow(1.0F - progress, 2.0) * this.targetY
            + 3.0 * Math.pow(progress, 2.0) * (1.0F - progress) * targetPoint.targetY
            + Math.pow(progress, 3.0) * targetPoint.startY
      );
   }
}
