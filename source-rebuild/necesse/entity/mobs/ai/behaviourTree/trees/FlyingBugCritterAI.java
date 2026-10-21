package necesse.entity.mobs.ai.behaviourTree.trees;

import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CritterRunAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.friendly.critters.CritterMob;

public class FlyingBugCritterAI<T extends CritterMob> extends SelectorAINode<T> {
   public FlyingBugCritterAI() {
      CritterRunAINode<T> runner = new CritterRunAINode<>();
      runner.runDistance = 256;
      this.addChild(runner);
      WandererAINode<T> wanderer = new WandererAINode<>(1500);
      wanderer.searchRadius = 4;
      this.addChild(wanderer);
   }
}
