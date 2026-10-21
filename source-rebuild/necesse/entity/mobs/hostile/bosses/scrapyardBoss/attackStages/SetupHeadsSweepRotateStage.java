package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;

public class SetupHeadsSweepRotateStage<T extends MutantHydraBossMob> extends AbstractHeadMoveWaitForArriveStage<T> {
   public int radius;
   public boolean reversed;

   public SetupHeadsSweepRotateStage(int radius, boolean reversed, MutantHydraHeadType... heads) {
      super(heads);
      this.radius = radius;
      this.reversed = reversed;
   }

   @Override
   public void setupHeadMovement(T mob, Blackboard<T> blackboard) {
      for (MutantHydraHeadType head : this.heads) {
         MutantHydraBossMob.HeadData headData = head.getter.apply(mob);
         MutantHydraHeadMob headMob = headData.getHeadMob();
         if (headMob != null) {
            headMob.setNewIdlePosition.set(false);
            headMob.setMovement(head.setupRotateStage(mob, headData, headMob, this.radius, 25, this.reversed));
         }
      }
   }
}
