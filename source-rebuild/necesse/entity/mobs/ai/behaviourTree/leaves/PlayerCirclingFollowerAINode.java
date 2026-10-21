package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.entity.mobs.Mob;

public class PlayerCirclingFollowerAINode<T extends Mob> extends CirclingFollowerAINode<T> {
   public PlayerCirclingFollowerAINode(int teleportDistance, int circlingRange) {
      super(teleportDistance, circlingRange);
   }

   @Override
   public Mob getFollowingMob(T mob) {
      return mob.getFollowingMob();
   }
}
