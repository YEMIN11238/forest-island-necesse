package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import necesse.engine.util.GameMath;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public class StartHeadsSweepRotateStage<T extends MutantHydraBossMob> extends AbstractHeadMoveWaitForArriveStage<T> {
   public int radius;
   public boolean reversed;
   public int minExtraTime;
   public int maxExtraTime;

   public StartHeadsSweepRotateStage(int radius, boolean reversed, int minExtraTime, int maxExtraTime, MutantHydraHeadType... heads) {
      super(heads);
      this.radius = radius;
      this.reversed = reversed;
      this.minExtraTime = minExtraTime;
      this.maxExtraTime = maxExtraTime;
   }

   @Override
   public void setupHeadMovement(T mob, Blackboard<T> blackboard) {
      int extraTime = GameMath.lerp(mob.getHealthPercent(), this.minExtraTime, this.maxExtraTime);

      for (MutantHydraHeadType head : this.heads) {
         MutantHydraBossMob.HeadData headData = head.getter.apply(mob);
         MutantHydraHeadMob headMob = headData.getHeadMob();
         if (headMob != null) {
            headMob.startRumbleAbility.runAndSend();
            headMob.setMovement(head.startRotateStage(mob, headData, headMob, this.radius, extraTime, 25, this.reversed));
         }
      }
   }
}
