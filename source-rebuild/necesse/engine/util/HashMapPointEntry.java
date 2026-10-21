package necesse.engine.util;

public interface HashMapPointEntry<K, V> {
   int getX();

   int getY();

   K getKey();

   V getValue();

   V setValue(V var1);
}
