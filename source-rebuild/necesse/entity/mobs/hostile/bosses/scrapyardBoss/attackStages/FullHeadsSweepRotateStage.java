package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageManagerNode;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public class FullHeadsSweepRotateStage<T extends MutantHydraBossMob> extends AttackStageManagerNode<T> {
   public FullHeadsSweepRotateStage(int minWaitTime, int maxWaitTime, boolean reversed, boolean setHeadIdleAfter, MutantHydraHeadType... heads) {
      this.addChild(new SetupHeadsSweepRotateStage<>(500, reversed, heads));
      this.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minWaitTime, maxWaitTime));
      this.addChild(new StartHeadsSweepRotateStage<>(500, reversed, 100, 500, heads));
      this.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minWaitTime, maxWaitTime));
      if (setHeadIdleAfter) {
         this.addChild(new SetHeadsIdleStage<>(heads));
      }
   }
}
