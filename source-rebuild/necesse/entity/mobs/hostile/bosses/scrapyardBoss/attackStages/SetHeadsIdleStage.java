package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public class SetHeadsIdleStage<T extends MutantHydraBossMob> extends AINode<T> implements AttackStageInterface<T> {
   public MutantHydraHeadType[] heads;

   public SetHeadsIdleStage(MutantHydraHeadType... heads) {
      this.heads = heads.length == 0 ? MutantHydraHeadType.values() : heads;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      for (MutantHydraHeadType head : this.heads) {
         MutantHydraBossMob.HeadData headData = head.getter.apply(mob);
         headData.runHeadMobIfPresent(MutantHydraHeadMob::returnToIdle);
      }
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return AINodeResult.SUCCESS;
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
