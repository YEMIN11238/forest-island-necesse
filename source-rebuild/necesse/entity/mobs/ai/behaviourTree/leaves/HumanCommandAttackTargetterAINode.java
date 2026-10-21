package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;

public class HumanCommandAttackTargetterAINode<T extends HumanMob> extends CommandAttackTargetterAINode<T> {
   public Mob getTarget(T mob) {
      return mob.commandAttackMob;
   }

   public void resetTarget(T mob) {
      mob.commandAttackMob = null;
   }

   public void tickTargetSet(T mob, Mob target) {
      if (mob.objectUser != null) {
         mob.objectUser.stopUsing();
      }
   }
}
