package necesse.engine.util.tween;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

public class Point2DTween extends ValueTween<Point2D, Point2DTween> {
   public Point2DTween(double duration, Point2D initialValue, Point2D endValue) {
      super(duration, initialValue, endValue);
   }

   public Point2DTween(Point2D initialValue) {
      super(initialValue);
   }

   public Point2DTween(Point2DTween existingTween, double duration, Point2D endValue) {
      super(existingTween, duration, endValue);
   }

   @Override
   protected void tween(double percent) {
      double startX = this.startValue.getX();
      double startY = this.startValue.getY();
      double x = startX + percent * (this.endValue.getX() - startX);
      double y = startY + percent * (this.endValue.getY() - startY);
      this.setValue(new Double(x, y));
   }

   public Point2DTween newTween(double duration, Point2D endValue) {
      return new Point2DTween(this, duration, endValue);
   }
}
