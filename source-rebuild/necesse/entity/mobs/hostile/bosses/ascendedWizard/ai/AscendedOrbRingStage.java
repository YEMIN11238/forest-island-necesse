package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;

public class AscendedOrbRingStage<T extends AscendedWizardMob> extends AINode<T> implements AttackStageInterface<T> {
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, "currentTarget");
      if (target != null) {
         mob.spawnOrbRingAbility.runAndSend(target, mob.isTransformed() ? 24 : 8, 5000, 2000, GameRandom.globalRandom.nextInt());
      }
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return AINodeResult.SUCCESS;
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
