package necesse.entity.mobs.ai.behaviourTree.event;

import java.awt.geom.Point2D.Float;

public class ConfuseWanderAIEvent extends AIEvent {
   public final boolean canDecreaseTimer;
   public final long confusionTimer;
   public final Float nextDirection;

   public ConfuseWanderAIEvent(boolean canDecreaseTimer, long confusionTimer, Float nextDirection) {
      this.canDecreaseTimer = canDecreaseTimer;
      this.confusionTimer = confusionTimer;
      this.nextDirection = nextDirection;
   }

   public ConfuseWanderAIEvent(long confusionTimer, Float nextDirection) {
      this(false, confusionTimer, nextDirection);
   }

   public ConfuseWanderAIEvent(long confusionTimer) {
      this(confusionTimer, null);
   }
}
