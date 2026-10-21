package necesse.engine.util;

import java.awt.Point;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class PointSetAbstract<T extends Set<Long>> implements Iterable<Point> {
   protected T set;

   public PointSetAbstract(T set) {
      this.set = set;
   }

   public static long toKey(int x, int y) {
      return GameMath.getUniqueLongKey(x, y);
   }

   public static int getX(long key) {
      return GameMath.getXFromUniqueLongKey(key);
   }

   public static int getY(long key) {
      return GameMath.getYFromUniqueLongKey(key);
   }

   public static Point toPoint(long key) {
      return new Point(getX(key), getY(key));
   }

   public int size() {
      return this.set.size();
   }

   public boolean isEmpty() {
      return this.set.isEmpty();
   }

   public boolean contains(int x, int y) {
      return this.set.contains(toKey(x, y));
   }

   public boolean add(int x, int y) {
      return this.set.add(toKey(x, y));
   }

   public boolean addAll(PointSetAbstract<?> other) {
      boolean changed = false;

      for (long key : other.set) {
         changed |= this.set.add(key);
      }

      return changed;
   }

   public boolean remove(int x, int y) {
      return this.set.remove(toKey(x, y));
   }

   public void clear() {
      this.set.clear();
   }

   @Override
   public Iterator<Point> iterator() {
      return GameUtils.mapIterator(this.set.iterator(), PointSetAbstract::toPoint);
   }

   public void forEach(BiConsumer<Integer, Integer> consumer) {
      for (long key : this.set) {
         consumer.accept(getX(key), getY(key));
      }
   }

   public Stream<Point> stream() {
      return this.set.stream().map(PointSetAbstract::toPoint);
   }

   public Iterable<Long> getKeys() {
      return this.set;
   }

   public T getUnderlyingSet() {
      return this.set;
   }

   public static <T extends PointSetAbstract<?>> Collector<Point, ?, T> collector(Supplier<T> supplier) {
      return Collector.of(supplier, (set, point) -> set.add(point.x, point.y), (set1, set2) -> {
         set1.addAll(set2);
         return set1;
      });
   }
}
