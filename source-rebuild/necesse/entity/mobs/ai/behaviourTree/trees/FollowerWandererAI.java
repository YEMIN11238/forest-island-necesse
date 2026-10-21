package necesse.entity.mobs.ai.behaviourTree.trees;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.FollowerAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;

public abstract class FollowerWandererAI<T extends Mob> extends SelectorAINode<T> {
   public final FollowerAINode<T> ropeFollowerAINode;
   public final WandererAINode<T> wandererAINode;

   public FollowerWandererAI(int teleportDistance, int stoppingDistance, int wanderFrequency) {
      this.addChild(this.ropeFollowerAINode = new FollowerAINode<T>(teleportDistance, stoppingDistance) {
         @Override
         public Mob getFollowingMob(T mob) {
            return FollowerWandererAI.this.getFollowingMob(mob);
         }
      });
      this.addChild(this.wandererAINode = new WandererAINode<>(wanderFrequency));
   }

   protected abstract Mob getFollowingMob(T var1);
}
