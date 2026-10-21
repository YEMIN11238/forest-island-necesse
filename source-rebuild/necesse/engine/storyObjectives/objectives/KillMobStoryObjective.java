package necesse.engine.storyObjectives.objectives;

import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.storyObjectives.listeners.MobKilledStoryObjectiveListener;
import necesse.entity.mobs.Mob;

public abstract class KillMobStoryObjective extends StoryObjective implements MobKilledStoryObjectiveListener {
   public final String mobStringID;
   public final boolean allowCompleteWhenNotCurrent;

   public KillMobStoryObjective(StoryObjectiveManager manager, String mobStringID, boolean allowCompleteWhenNotCurrent) {
      super(manager);
      this.mobStringID = mobStringID;
      this.allowCompleteWhenNotCurrent = allowCompleteWhenNotCurrent;
   }

   @Override
   public void onMobKilled(Mob mob) {
      if (!this.isCompleted()) {
         if (this.allowCompleteWhenNotCurrent || this.isCurrentObjective()) {
            if (mob.getStringID().equals(this.mobStringID)) {
               this.markCompleted();
            }
         }
      }
   }
}
