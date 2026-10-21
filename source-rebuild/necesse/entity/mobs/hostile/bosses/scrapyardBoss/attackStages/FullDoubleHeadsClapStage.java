package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageManagerNode;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public class FullDoubleHeadsClapStage<T extends MutantHydraBossMob> extends AttackStageManagerNode<T> {
   public FullDoubleHeadsClapStage(int minWaitTime, int maxWaitTime, boolean reversed, boolean setHeadIdleAfter, MutantHydraHeadType... heads) {
      if (reversed) {
         this.addChild(new SetupHeadsSweepStage<>(500, true, heads));
      } else {
         this.addChild(new SetupHeadsSweepStage<>(500, false, heads));
      }

      this.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minWaitTime, maxWaitTime));
      if (reversed) {
         this.addChild(new StartHeadsSweepStage<>(500, true, 100, 500, heads));
      } else {
         this.addChild(new StartHeadsSweepStage<>(500, false, 100, 500, heads));
      }

      this.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minWaitTime, maxWaitTime));
      if (reversed) {
         this.addChild(new StartHeadsSweepStage<>(500, false, 100, 500, heads));
      } else {
         this.addChild(new StartHeadsSweepStage<>(500, true, 100, 500, heads));
      }

      this.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minWaitTime, maxWaitTime));
      if (setHeadIdleAfter) {
         this.addChild(new SetHeadsIdleStage<>(heads));
      }
   }
}
