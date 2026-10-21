package necesse.engine.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ObjectKeyMap<K, V> {
   private final Function<V, K> keyMapper;
   private final Map<K, V> map;

   public ObjectKeyMap(Map<K, V> map, Function<V, K> keyMapper) {
      this.map = map;
      this.keyMapper = keyMapper;
   }

   public ObjectKeyMap(Function<V, K> keyMapper) {
      this(new HashMap<>(), keyMapper);
   }

   public ObjectKeyMap(int initialCapacity, Function<V, K> keyMapper) {
      this(new HashMap<>(initialCapacity), keyMapper);
   }

   public void clear() {
      this.map.clear();
   }

   public int size() {
      return this.map.size();
   }

   public V get(K key) {
      return this.map.get(key);
   }

   public V getOrDefault(K key, V defaultValue) {
      return this.map.getOrDefault(key, defaultValue);
   }

   public V put(V value) {
      return this.map.put(this.keyMapper.apply(value), value);
   }

   public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
      return this.map.compute(key, remappingFunction);
   }

   public void putAll(Collection<V> values) {
      for (V value : values) {
         this.put(value);
      }
   }

   public boolean containsKey(K key) {
      return this.map.containsKey(key);
   }

   public V remove(K key) {
      return this.map.remove(key);
   }

   public Set<K> keySet() {
      return this.map.keySet();
   }

   public Collection<V> values() {
      return this.map.values();
   }

   public Set<Entry<K, V>> entrySet() {
      return this.map.entrySet();
   }
}
