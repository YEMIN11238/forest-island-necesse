package necesse.engine.util.tween;

public class IntTween extends ValueTween<Integer, IntTween> {
   public IntTween(double duration, int initialValue, int endValue) {
      super(duration, initialValue, endValue);
   }

   public IntTween(int initialValue) {
      super(initialValue);
   }

   public IntTween(IntTween existingTween, double duration, int endValue) {
      super(existingTween, duration, endValue);
   }

   @Override
   protected void tween(double percent) {
      this.setValue((int)Math.round(this.startValue.intValue() + percent * (this.endValue - this.startValue)));
   }

   public IntTween newTween(double duration, Integer endValue) {
      return new IntTween(this, duration, endValue);
   }
}
