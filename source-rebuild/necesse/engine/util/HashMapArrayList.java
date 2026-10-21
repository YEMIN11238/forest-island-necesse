package necesse.engine.util;

import java.util.ArrayList;

public class HashMapArrayList<K, V> extends HashMapCollection<K, V, ArrayList<V>> {
   public HashMapArrayList() {
      super(ArrayList::new);
   }

   public HashMapArrayList(int initialSize) {
      super(() -> new ArrayList<>(initialSize));
   }
}
