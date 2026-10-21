package necesse.entity.mobs.ai.behaviourTree.event;

import necesse.entity.mobs.MobBeforeHitEvent;

public class AIBeforeHitEvent extends AIEvent {
   public final MobBeforeHitEvent event;

   public AIBeforeHitEvent(MobBeforeHitEvent event) {
      this.event = event;
   }
}
