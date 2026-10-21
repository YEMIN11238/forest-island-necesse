package necesse.engine.storyObjectives.objectives;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.playerStats.PlayerStats;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.storyObjectives.listeners.IncursionCompletedObjectiveListener;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormStoryObjectiveComponent;
import necesse.level.maps.incursion.BiomeMissionIncursionData;

public abstract class CompleteIncursionTierStoryObjective extends StoryObjective implements IncursionCompletedObjectiveListener {
   public int minIncursionTier;
   public String migrateIfObtainedItemStringID;

   public CompleteIncursionTierStoryObjective(StoryObjectiveManager manager, int minIncursionTier, String migrateIfObtainedItemStringID) {
      super(manager);
      this.minIncursionTier = minIncursionTier;
      this.migrateIfObtainedItemStringID = migrateIfObtainedItemStringID;
   }

   public boolean doesIncursionCountAsComplete(BiomeMissionIncursionData incursion) {
      return incursion.getTabletTier() >= this.minIncursionTier;
   }

   public boolean migrationShouldMarkAsComplete(Server server, PlayerStats characterStats) {
      return characterStats.completed_incursions.getTotalTiersAbove(this.minIncursionTier, true) > 0;
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
   public void onIncursionCompleted(BiomeMissionIncursionData incursion) {
      if (!this.isCompleted()) {
         if (this.doesIncursionCountAsComplete(incursion)) {
            this.markCompleted();
         }
      }
   }

   @Override
   public void setupFormComponent(FormStoryObjectiveComponent form, FormFlow flow) {
      form.addTitle(flow, new LocalMessage("objectives", "majorobjective"));
      flow.next(2);
      LocalMessage message = new LocalMessage("quests", "completeincursiontier", "tier", this.minIncursionTier);
      form.addObjective(flow, -1, message, this.isCompleted());
      if (!this.isCompleted()) {
         LocalMessage tip = new LocalMessage(
            "objectives",
            "incursionsfallenaltartip",
            "fallenaltar",
            TypeParsers.getItemAndNameParseString("fallenaltar"),
            "gatewaytablet",
            TypeParsers.getItemAndNameParseString("gatewaytablet")
         );
         form.addObjectiveNote(flow, tip, this.isCompleted());
      }

      flow.next(2);
      form.addClaimText(flow);
      form.addRewardsText(flow);
   }
}
