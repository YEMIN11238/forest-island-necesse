package necesse.engine.util.tween;

public class DoubleTween extends ValueTween<Double, DoubleTween> {
   public DoubleTween(double duration, double initialValue, double endValue) {
      super(duration, initialValue, endValue);
   }

   public DoubleTween(double initialValue) {
      super(initialValue);
   }

   public DoubleTween(DoubleTween existingTween, double duration, double endValue) {
      super(existingTween, duration, endValue);
   }

   @Override
   protected void tween(double percent) {
      this.setValue(this.startValue + percent * (this.endValue - this.startValue));
   }

   public DoubleTween newTween(double duration, Double endValue) {
      return new DoubleTween(this, duration, endValue);
   }
}
