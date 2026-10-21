package necesse.engine.util;

public class ObjectDistancePair<T> {
   public final T object;
   public final float distance;

   public ObjectDistancePair(T object, float distance) {
      this.object = object;
      this.distance = distance;
   }
}
