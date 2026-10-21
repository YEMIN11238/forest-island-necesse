package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.entity.mobs.Mob;

public class PlayerFollowerAINode<T extends Mob> extends FollowerAINode<T> {
   public PlayerFollowerAINode(int teleportDistance, int stoppingDistance) {
      super(teleportDistance, stoppingDistance);
   }

   @Override
   public Mob getFollowingMob(T mob) {
      return mob.getFollowingMob();
   }
}
