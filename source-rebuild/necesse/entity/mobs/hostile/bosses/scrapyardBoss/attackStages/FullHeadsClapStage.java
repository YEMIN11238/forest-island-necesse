package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageManagerNode;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public class FullHeadsClapStage<T extends MutantHydraBossMob> extends AttackStageManagerNode<T> {
   public FullHeadsClapStage(int minWaitTime, int maxWaitTime, boolean vertical, boolean setHeadIdleAfter, MutantHydraHeadType... heads) {
      if (vertical) {
         this.addChild(new SetupHeadsSweepStage<>(500, true, heads));
         this.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minWaitTime, maxWaitTime));
         this.addChild(new StartHeadsSweepStage<>(500, true, 100, 500, heads));
         this.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minWaitTime, maxWaitTime));
      } else {
         this.addChild(new SetupHeadsSweepStage<>(500, false, heads));
         this.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minWaitTime, maxWaitTime));
         this.addChild(new StartHeadsSweepStage<>(500, false, 100, 500, heads));
         this.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minWaitTime, maxWaitTime));
      }

      if (setHeadIdleAfter) {
         this.addChild(new SetHeadsIdleStage<>(heads));
      }
   }
}
