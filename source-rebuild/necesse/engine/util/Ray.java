package necesse.engine.util;

import java.awt.geom.Line2D.Double;

public class Ray<T> extends Double {
   public final double dist = this.getP1().distance(this.getP2());
   public final T targetHit;
   public final IntersectionPoint.Dir targetHitDir;

   public Ray(double x1, double y1, double x2, double y2, T targetHit, IntersectionPoint.Dir targetHitDir) {
      super(x1, y1, x2, y2);
      this.targetHit = targetHit;
      this.targetHitDir = targetHitDir;
   }

   public IntersectionPoint<T> getIntersectionPoint() {
      return new IntersectionPoint<>(this.x2, this.y2, this.targetHit, this.targetHitDir);
   }
}
