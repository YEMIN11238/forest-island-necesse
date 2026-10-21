package necesse.entity.mobs.ai.behaviourTree.event;

import necesse.entity.mobs.MobBeforeHitCalculatedEvent;

public class AIBeforeHitCalculatedEvent extends AIEvent {
   public final MobBeforeHitCalculatedEvent event;

   public AIBeforeHitCalculatedEvent(MobBeforeHitCalculatedEvent event) {
      this.event = event;
   }
}
