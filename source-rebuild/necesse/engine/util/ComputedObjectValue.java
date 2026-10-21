package necesse.engine.util;

import java.util.function.Supplier;

public class ComputedObjectValue<T, V> extends ComputedValue<V> {
   public final T object;

   public ComputedObjectValue(T object, Supplier<V> supplier) {
      super(supplier);
      this.object = object;
   }
}
