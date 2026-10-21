package necesse.engine.util.tween;

@FunctionalInterface
public interface EaseFunction {
   double ease(double var1);

   default float ease(float p) {
      return (float)this.ease((double)p);
   }
}
