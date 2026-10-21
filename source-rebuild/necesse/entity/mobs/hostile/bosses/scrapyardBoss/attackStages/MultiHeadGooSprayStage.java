package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import java.util.function.Function;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.SimultaneousAttackStageManagerNode;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public class MultiHeadGooSprayStage<T extends MutantHydraBossMob> extends SimultaneousAttackStageManagerNode<T> {
   public MultiHeadGooSprayStage(Function<T, Integer> totalTimeGetter, int minShakeTime, int maxShakeTime, MutantHydraHeadType... heads) {
      for (MutantHydraHeadType head : heads) {
         this.addChild(new HeadShakeAndGooSprayStage<>(head, totalTimeGetter, minShakeTime, maxShakeTime));
      }
   }
}
