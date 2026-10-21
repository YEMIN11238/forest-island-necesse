package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import java.util.function.Function;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public class StartHeadShakeStage<T extends MutantHydraBossMob> extends AINode<T> implements AttackStageInterface<T> {
   public MutantHydraHeadType head;
   public Function<T, Integer> durationGetter;
   public boolean waitForEnding;
   protected long endTime;

   public StartHeadShakeStage(MutantHydraHeadType head, Function<T, Integer> durationGetter, boolean waitForEnding) {
      this.head = head;
      this.durationGetter = durationGetter;
      this.waitForEnding = waitForEnding;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.endTime = 0L;
      MutantHydraHeadMob headMob = this.head.getter.apply(mob).getHeadMob();
      if (headMob != null) {
         int duration = this.durationGetter.apply(mob);
         headMob.startShakeAnimation.runAndSend(duration);
         headMob.setNewIdlePosition.set(false);
         this.endTime = this.waitForEnding ? mob.getTime() + duration : 0L;
      }
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return this.endTime <= mob.getTime() ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
