package necesse.engine.util.tween;

public class FloatTween extends ValueTween<Float, FloatTween> {
   public FloatTween(double duration, float initialValue, float endValue) {
      super(duration, initialValue, endValue);
   }

   public FloatTween(float initialValue) {
      super(initialValue);
   }

   public FloatTween(FloatTween existingTween, double duration, float endValue) {
      super(existingTween, duration, endValue);
   }

   public FloatTween newTween(double duration, Float endValue) {
      return new FloatTween(this, duration, endValue);
   }

   @Override
   protected void tween(double percent) {
      this.setValue((float)(this.startValue.floatValue() + percent * (this.endValue - this.startValue)));
   }
}
