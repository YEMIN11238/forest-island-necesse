package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MutantHydraHeadScreamLevelEvent;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public class HeadScreamStage<T extends MutantHydraBossMob> extends AINode<T> implements AttackStageInterface<T> {
   public MutantHydraHeadType head;
   public boolean waitForEvent;
   public MutantHydraHeadScreamLevelEvent currentEvent;

   public HeadScreamStage(MutantHydraHeadType head, boolean waitForEvent) {
      this.head = head;
      this.waitForEvent = waitForEvent;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      MutantHydraHeadMob headMob = this.head.getter.apply(mob).getHeadMob();
      if (headMob != null) {
         this.currentEvent = new MutantHydraHeadScreamLevelEvent(headMob, GameRandom.globalRandom.nextInt());
         mob.getLevel().entityManager.events.add(this.currentEvent);
      }
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return this.waitForEvent && this.currentEvent != null && !this.currentEvent.isOver() ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
