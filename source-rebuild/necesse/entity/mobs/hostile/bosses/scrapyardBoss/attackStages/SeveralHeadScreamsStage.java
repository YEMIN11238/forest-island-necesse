package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageManagerNode;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public class SeveralHeadScreamsStage<T extends MutantHydraBossMob> extends AttackStageManagerNode<T> {
   public SeveralHeadScreamsStage(MutantHydraHeadType head, int count, int minWaitTime, int maxWaitTime) {
      for (int i = 0; i < count; i++) {
         this.addChild(new HeadScreamStage<>(head, false));
         this.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minWaitTime, maxWaitTime));
      }
   }
}
