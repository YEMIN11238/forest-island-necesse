package necesse.entity.mobs.ai.behaviourTree.event;

import necesse.entity.mobs.MobWasHitEvent;

public class AIWasHitEvent extends AIEvent {
   public final MobWasHitEvent event;

   public AIWasHitEvent(MobWasHitEvent event) {
      this.event = event;
   }
}
