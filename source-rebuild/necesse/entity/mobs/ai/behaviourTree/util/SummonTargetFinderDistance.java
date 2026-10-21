package necesse.entity.mobs.ai.behaviourTree.util;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;

public class SummonTargetFinderDistance<T extends Mob> extends TargetFinderDistance<T> {
   public SummonTargetFinderDistance(int searchDistance) {
      super(searchDistance);
   }

   @Override
   public float getSearchDistanceMod(T mob, Mob target) {
      float ownerMod = mob.buffManager.getModifier(BuffModifiers.CHASER_RANGE);
      float targetMod = target == null ? 1.0F : target.buffManager.getModifier(BuffModifiers.TARGET_RANGE);
      Mob following = mob.getFollowingMob();
      float followingMod = following == null ? 1.0F : following.buffManager.getModifier(BuffModifiers.SUMMONS_TARGET_RANGE);
      return targetMod * followingMod * ownerMod;
   }
}
