package necesse.entity.mobs.ai.behaviourTree.event;

import java.awt.Point;
import java.util.function.Supplier;

public class StartStaringAIEvent extends AIEvent {
   public int totalTime;
   public Supplier<Point> levelPositionGetter;

   public StartStaringAIEvent(int totalTime, Supplier<Point> levelPositionGetter) {
      this.totalTime = totalTime;
      this.levelPositionGetter = levelPositionGetter;
   }
}
