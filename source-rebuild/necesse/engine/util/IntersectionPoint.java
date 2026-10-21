package necesse.engine.util;

import java.awt.geom.Point2D.Double;

public class IntersectionPoint<T> extends Double {
   public final T target;
   public final IntersectionPoint.Dir dir;

   public IntersectionPoint(double x, double y, T target, IntersectionPoint.Dir dir) {
      super(x, y);
      this.target = target;
      this.dir = dir;
   }

   public enum Dir {
      UP,
      RIGHT,
      DOWN,
      LEFT;
   }
}
