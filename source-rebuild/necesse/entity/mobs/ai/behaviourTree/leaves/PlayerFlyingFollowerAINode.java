package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.entity.mobs.Mob;

public class PlayerFlyingFollowerAINode<T extends Mob> extends FlyingFollowerAINode<T> {
   public PlayerFlyingFollowerAINode(int teleportDistance, int stoppingDistance) {
      super(teleportDistance, stoppingDistance);
   }

   @Override
   public Mob getFollowingMob(T mob) {
      return mob.getFollowingMob();
   }
}
