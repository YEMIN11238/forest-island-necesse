package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public abstract class AbstractHeadMoveWaitForArriveStage<T extends MutantHydraBossMob> extends AINode<T> implements AttackStageInterface<T> {
   public MutantHydraHeadType[] heads;

   public AbstractHeadMoveWaitForArriveStage(MutantHydraHeadType... heads) {
      this.heads = heads;
   }

   public abstract void setupHeadMovement(T var1, Blackboard<T> var2);

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.setupHeadMovement(mob, blackboard);
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      for (MutantHydraHeadType head : this.heads) {
         MutantHydraHeadMob headMob = head.getter.apply(mob).getHeadMob();
         if (headMob == null || !headMob.hasArrivedAtTarget()) {
            return AINodeResult.RUNNING;
         }
      }

      return AINodeResult.SUCCESS;
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
