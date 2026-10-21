package necesse.engine.util;

public class ComputedObjectFloat<T> extends ComputedFloat {
   public final T object;

   public ComputedObjectFloat(T object, ComputedFloat.FloatSupplier supplier) {
      super(supplier);
      this.object = object;
   }
}
