package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Supplier;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.RelayAttackStageAINode;

public class AscendedRandomBossAttackStage<T extends AscendedWizardMob> extends RelayAttackStageAINode<T> {
   public static ArrayList<Supplier<AINode<? extends AscendedWizardMob>>> possibleStages = new ArrayList<>();
   public ArrayList<Integer> currentRotationIndexes;

   public AscendedRandomBossAttackStage(ArrayList<Integer> rotationTracker) {
      if (rotationTracker != null) {
         this.currentRotationIndexes = rotationTracker;
      } else {
         this.currentRotationIndexes = new ArrayList<>();
      }
   }

   @Override
   public AINode<T> getNextNode() {
      if (this.currentRotationIndexes.isEmpty()) {
         for (int i = 0; i < possibleStages.size(); i++) {
            this.currentRotationIndexes.add(i);
         }

         Collections.shuffle(this.currentRotationIndexes, GameRandom.globalRandom);
      }

      int index = this.currentRotationIndexes.remove(0);
      return (AINode<T>)possibleStages.get(index).get();
   }

   static {
      possibleStages.add(AscendedMoonlightDancerStage::new);
      possibleStages.add(AscendedMotherSlimeStage::new);
      possibleStages.add(AscendedSpiderEmpressStage::new);
      possibleStages.add(AscendedSunlightChampionStage::new);
      possibleStages.add(AscendedCrystalDragonGolemsStage::new);
      possibleStages.add(AscendedCrystalDragonShardsStage::new);
      possibleStages.add(AscendedNightSwarmStage::new);
   }
}
