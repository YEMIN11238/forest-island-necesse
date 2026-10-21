package necesse.engine.util;

import java.util.Collections;
import java.util.Set;

public class SingletonPointSet extends PointSetAbstract<Set<Long>> {
   public SingletonPointSet(int x, int y) {
      super(Collections.singleton(GameMath.getUniqueLongKey(x, y)));
   }
}
