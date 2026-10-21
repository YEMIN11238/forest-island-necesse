package necesse.engine.storyObjectives.objectives.bossObjectives;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.server.Server;
import necesse.engine.playerStats.PlayerStats;
import necesse.engine.registries.MobRegistry;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.storyObjectives.listeners.MobKilledStatAddedStoryObjectiveListener;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormStoryObjectiveComponent;

public abstract class DefeatBossStoryObjective extends StoryObjective implements MobKilledStatAddedStoryObjectiveListener {
   public String mobStringID;
   public String migrateIfObtainedItemStringID;

   public DefeatBossStoryObjective(StoryObjectiveManager manager, String mobStringID, String migrateIfObtainedItemStringID) {
      super(manager);
      this.mobStringID = mobStringID;
      this.migrateIfObtainedItemStringID = migrateIfObtainedItemStringID;
   }

   public boolean doesMobKilledCountAsComplete(String mobStringID) {
      return mobStringID.equals(this.mobStringID);
   }

   public boolean migrationShouldMarkAsComplete(Server server, PlayerStats characterStats) {
      return characterStats.mob_kills.getKills(this.mobStringID) > 0;
   }

   public boolean migrationShouldMarkAsClaimed(Server server, PlayerStats characterStats) {
      return this.migrateIfObtainedItemStringID != null && characterStats.items_obtained.isItemObtained(this.migrateIfObtainedItemStringID);
   }

   @Override
   protected void migrateFromCharacterStats(Server server, PlayerStats characterStats, Runnable markComplete, Runnable markClaimed) {
      super.migrateFromCharacterStats(server, characterStats, markComplete, markClaimed);
      if (this.migrationShouldMarkAsComplete(server, characterStats)) {
         markComplete.run();
         if (this.migrationShouldMarkAsClaimed(server, characterStats)) {
            markClaimed.run();
         }
      }
   }

   @Override
   public void onMobStatAdded(String mobStringID) {
      if (!this.isCompleted()) {
         if (this.doesMobKilledCountAsComplete(mobStringID)) {
            this.markCompleted();
         }
      }
   }

   @Override
   public void setupFormComponent(FormStoryObjectiveComponent form, FormFlow flow) {
      form.addTitle(flow, new LocalMessage("objectives", "majorobjective"));
      flow.next(2);
      LocalMessage message = new LocalMessage("objectives", "defeatmob", "mob", TypeParsers.getMobAndNameParseString(this.mobStringID));
      form.addObjective(flow, -1, message, this.isCompleted());
      if (!this.isCompleted()) {
         String hint = MobRegistry.getKillHint(this.mobStringID);
         if (hint != null) {
            form.addObjectiveNote(flow, new StaticMessage(hint), this.isCompleted());
         }
      }

      flow.next(2);
      form.addClaimText(flow);
      form.addRewardsText(flow);
   }
}
