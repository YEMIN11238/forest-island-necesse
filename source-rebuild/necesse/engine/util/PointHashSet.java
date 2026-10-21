package necesse.engine.util;

import java.awt.Point;
import java.util.HashSet;
import java.util.stream.Collector;

public class PointHashSet extends PointSetAbstract<HashSet<Long>> {
   public PointHashSet() {
      super(new HashSet<>());
   }

   public PointHashSet(int initialCapacity) {
      super(new HashSet<>(initialCapacity));
   }

   public PointHashSet(PointSetAbstract<?> set) {
      this(set.size());
      this.addAll(set);
   }

   public static Collector<Point, ?, PointHashSet> collector() {
      return PointSetAbstract.collector(PointHashSet::new);
   }
}
