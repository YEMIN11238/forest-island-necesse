package necesse.engine.util;

public class IndexedObject<T> {
   public final int index;
   public final T object;

   public IndexedObject(int index, T object) {
      this.index = index;
      this.object = object;
   }
}
