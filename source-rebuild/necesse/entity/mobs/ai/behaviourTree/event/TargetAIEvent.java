package necesse.entity.mobs.ai.behaviourTree.event;

import necesse.entity.mobs.Mob;

public class TargetAIEvent extends AIEvent {
   public final Mob target;

   public TargetAIEvent(Mob target) {
      this.target = target;
   }
}
