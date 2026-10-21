package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public abstract class AbstractHeadMoveTimedStage<T extends MutantHydraBossMob> extends AINode<T> implements AttackStageInterface<T> {
   public MutantHydraHeadType[] heads;
   public long endTime;

   public AbstractHeadMoveTimedStage(MutantHydraHeadType... heads) {
      this.heads = heads;
   }

   public abstract int setupHeadMovement(T var1, Blackboard<T> var2);

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.endTime = mob.getTime() + this.setupHeadMovement(mob, blackboard);
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return mob.getTime() >= this.endTime ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
