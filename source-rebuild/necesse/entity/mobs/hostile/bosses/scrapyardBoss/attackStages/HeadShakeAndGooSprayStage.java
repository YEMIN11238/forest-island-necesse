package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import java.util.function.Function;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageManagerNode;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public class HeadShakeAndGooSprayStage<T extends MutantHydraBossMob> extends AttackStageManagerNode<T> {
   public HeadShakeAndGooSprayStage(MutantHydraHeadType head, Function<T, Integer> totalTimeGetter, int minShakeTime, int maxShakeTime) {
      this.addChild(new StartHeadShakeStage<>(head, m -> GameMath.lerp(m.getHealthPercent(), minShakeTime, maxShakeTime), true));
      this.addChild(new HeadGooSprayStage<>(head, totalTimeGetter));
   }
}
