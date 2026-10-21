package necesse.engine.util;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class HashMapCollection<K, V, L extends Collection<V>> extends HashMapCollectionAbstract<K, V, L> {
   private Supplier<L> listCreator;

   public HashMapCollection(Supplier<L> listCreator) {
      this.listCreator = listCreator;
   }

   protected L createNewList() {
      return this.listCreator.get();
   }

   protected void addToList(L collection, V value) {
      collection.add(value);
   }

   protected void addAllToList(L collection, Collection<V> values) {
      collection.addAll(values);
   }

   protected Stream<V> streamValues(L collection) {
      return collection.stream();
   }

   protected boolean listContains(L collection, V value) {
      return collection.contains(value);
   }

   protected boolean isListEmpty(L collection) {
      return collection.isEmpty();
   }

   protected int getListSize(L collection) {
      return collection.size();
   }

   protected boolean removeFromList(L collection, V value) {
      return collection.remove(value);
   }

   protected boolean removeAllFromList(L collection, Collection<V> values) {
      return collection.removeAll(values);
   }
}
