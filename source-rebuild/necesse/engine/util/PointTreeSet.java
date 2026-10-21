package necesse.engine.util;

import java.awt.Point;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collector;

public class PointTreeSet extends PointSetAbstract<TreeSet<Long>> implements Zoning.ToRectangleInterface {
   public PointTreeSet(Comparator<Point> comparator) {
      super(new TreeSet<>((key1, key2) -> {
         int x1 = GameMath.getXFromUniqueLongKey(key1);
         int y1 = GameMath.getYFromUniqueLongKey(key1);
         int x2 = GameMath.getXFromUniqueLongKey(key2);
         int y2 = GameMath.getYFromUniqueLongKey(key2);
         return comparator.compare(new Point(x1, y1), new Point(x2, y2));
      }));
   }

   public static Comparator<Point> firstYThenXComparator() {
      Comparator<Point> comparator = Comparator.comparingInt(p -> p.y);
      return comparator.thenComparingInt(p -> p.x);
   }

   public NavigableSet<Point> subSet(Point fromElement, boolean fromInclusive, Point toElement, boolean toInclusive) {
      return new PointNavigableSet(this.set.subSet(toKey(fromElement.x, fromElement.y), fromInclusive, toKey(toElement.x, toElement.y), toInclusive));
   }

   public NavigableSet<Point> headSet(Point toElement, boolean inclusive) {
      return new PointNavigableSet(this.set.headSet(toKey(toElement.x, toElement.y), inclusive));
   }

   public NavigableSet<Point> tailSet(Point fromElement, boolean inclusive) {
      return new PointNavigableSet(this.set.tailSet(toKey(fromElement.x, fromElement.y), inclusive));
   }

   public SortedSet<Point> subSet(Point fromElement, Point toElement) {
      return new PointSortedSet(this.set.subSet(toKey(fromElement.x, fromElement.y), toKey(toElement.x, toElement.y)));
   }

   public SortedSet<Point> headSet(Point toElement) {
      return new PointSortedSet(this.set.headSet(toKey(toElement.x, toElement.y)));
   }

   public SortedSet<Point> tailSet(Point fromElement) {
      return new PointSortedSet(this.set.tailSet(toKey(fromElement.x, fromElement.y)));
   }

   public NavigableSet<Point> ascendingSet() {
      return new PointNavigableSet(this.set);
   }

   public NavigableSet<Point> descendingSet() {
      return new PointNavigableSet(this.set.descendingSet());
   }

   @Override
   public Point first() {
      return toPoint(this.set.first());
   }

   public Point last() {
      return toPoint(this.set.last());
   }

   public Point lower(int x, int y) {
      Long key = this.set.lower(GameMath.getUniqueLongKey(x, y));
      return key == null ? null : toPoint(key);
   }

   public Point floor(int x, int y) {
      Long key = this.set.floor(GameMath.getUniqueLongKey(x, y));
      return key == null ? null : toPoint(key);
   }

   public Point ceiling(int x, int y) {
      Long key = this.set.ceiling(GameMath.getUniqueLongKey(x, y));
      return key == null ? null : toPoint(key);
   }

   @Override
   public Point higher(int x, int y) {
      Long key = this.set.higher(GameMath.getUniqueLongKey(x, y));
      return key == null ? null : toPoint(key);
   }

   public Point pollFirst() {
      Long first = this.set.pollFirst();
      return first == null ? null : toPoint(first);
   }

   public Point pollLast() {
      Long first = this.set.pollLast();
      return first == null ? null : toPoint(first);
   }

   public static Collector<Point, ?, PointTreeSet> collector(Comparator<Point> comparator) {
      return PointSetAbstract.collector(() -> new PointTreeSet(comparator));
   }
}
