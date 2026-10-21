package necesse.engine.util;

import java.awt.Point;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

public class PointSortedSet implements SortedSet<Point> {
   protected final SortedSet<Long> set;

   public PointSortedSet(SortedSet<Long> set) {
      this.set = set;
   }

   protected long toKey(int x, int y) {
      return GameMath.getUniqueLongKey(x, y);
   }

   protected int getX(long key) {
      return GameMath.getXFromUniqueLongKey(key);
   }

   protected int getY(long key) {
      return GameMath.getYFromUniqueLongKey(key);
   }

   protected Point toPoint(long key) {
      return new Point(this.getX(key), this.getY(key));
   }

   @Override
   public Comparator<? super Point> comparator() {
      Comparator<? super Long> comparator = this.set.comparator();
      return comparator == null ? null : (o1, o2) -> comparator.compare(this.toKey(o1.x, o1.y), this.toKey(o2.x, o2.y));
   }

   public SortedSet<Point> subSet(Point fromElement, Point toElement) {
      SortedSet<Long> subSet = this.set.subSet(this.toKey(fromElement.x, fromElement.y), this.toKey(toElement.x, toElement.y));
      return new PointSortedSet(subSet);
   }

   public SortedSet<Point> headSet(Point toElement) {
      SortedSet<Long> headSet = this.set.headSet(this.toKey(toElement.x, toElement.y));
      return new PointSortedSet(headSet);
   }

   public SortedSet<Point> tailSet(Point fromElement) {
      SortedSet<Long> tailSet = this.set.tailSet(this.toKey(fromElement.x, fromElement.y));
      return new PointSortedSet(tailSet);
   }

   public Point first() {
      Long first = this.set.first();
      return first == null ? null : this.toPoint(first);
   }

   public Point last() {
      Long last = this.set.last();
      return last == null ? null : this.toPoint(last);
   }

   @Override
   public int size() {
      return this.set.size();
   }

   @Override
   public boolean isEmpty() {
      return this.set.isEmpty();
   }

   @Override
   public boolean contains(Object o) {
      if (o instanceof Point) {
         Point point = (Point)o;
         return this.set.contains(this.toKey(point.x, point.y));
      } else {
         return false;
      }
   }

   @Override
   public Iterator<Point> iterator() {
      return GameUtils.mapIterator(this.set.iterator(), this::toPoint);
   }

   @Override
   public Object[] toArray() {
      return this.set.toArray();
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return (T[])this.set.toArray(a);
   }

   public boolean add(Point point) {
      return this.set.add(this.toKey(point.x, point.y));
   }

   @Override
   public boolean remove(Object o) {
      if (o instanceof Point) {
         Point point = (Point)o;
         return this.set.remove(this.toKey(point.x, point.y));
      } else {
         return false;
      }
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      for (Object o : c) {
         if (!(o instanceof Point)) {
            return false;
         }

         Point point = (Point)o;
         if (!this.set.contains(this.toKey(point.x, point.y))) {
            return false;
         }
      }

      return true;
   }

   @Override
   public boolean addAll(Collection<? extends Point> c) {
      boolean changed = false;

      for (Point point : c) {
         changed |= this.add(point);
      }

      return changed;
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      boolean changed = false;

      for (Object o : c) {
         if (o instanceof Point) {
            Point point = (Point)o;
            if (!this.set.contains(this.toKey(point.x, point.y))) {
               changed |= this.remove(point);
            }
         } else {
            changed |= this.remove(o);
         }
      }

      return changed;
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      boolean changed = false;

      for (Object o : c) {
         if (o instanceof Point) {
            Point point = (Point)o;
            changed |= this.remove(point);
         } else {
            changed |= this.remove(o);
         }
      }

      return changed;
   }

   @Override
   public void clear() {
      this.set.clear();
   }
}
