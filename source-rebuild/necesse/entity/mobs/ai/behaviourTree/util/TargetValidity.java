package necesse.entity.mobs.ai.behaviourTree.util;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;

public class TargetValidity<T extends Mob> {
   public boolean isValidTarget(AINode<T> node, T mob, Mob target, boolean isNewTarget) {
      return target != null && !target.removed() && target.isVisible() && mob.isSamePlace(target) && target.canBeTargeted(mob, null);
   }
}
