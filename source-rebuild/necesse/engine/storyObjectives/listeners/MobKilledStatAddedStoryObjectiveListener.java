package necesse.engine.storyObjectives.listeners;

import necesse.entity.mobs.Mob;

public interface MobKilledStatAddedStoryObjectiveListener {
   void onMobStatAdded(String var1);

   default void onMobStatAdded(Mob mob) {
      this.onMobStatAdded(mob.getStringID());
   }
}
