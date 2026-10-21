package necesse.level.maps.levelData.settlementData;

import java.util.HashSet;
import necesse.engine.GameLog;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.StoryObjectiveRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.storyObjectives.StoryObjectiveManager;

public class SettlementStoryObjectives {
   protected final ServerSettlementData settlementData;
   private HashSet<String> storyObjectivesCompleted = new HashSet<>();
   private int highestMajorObjectiveIndexCompleted = -1;
   private int highestObjectiveIndexCompleted = -1;
   private int totalMajorObjectivesCompleted = 0;
   private final HashSet<Long> calculatedAuths = new HashSet<>();

   public SettlementStoryObjectives(ServerSettlementData settlementData) {
      this.settlementData = settlementData;
   }

   public void addSaveData(SaveData save) {
      save.addStringHashSet("storyObjectivesCompleted", this.storyObjectivesCompleted);
   }

   public void applySaveData(LoadData save) {
      this.storyObjectivesCompleted = save.getStringHashSet("storyObjectivesCompleted", new HashSet<>(), false);
      LoadData questData = save.getFirstLoadDataByName("clientQuests");
      if (questData != null) {
         for (LoadData clientData : questData.getLoadDataByName("quests")) {
            try {
               HashSet<String> objectiveStringIDs = SettlementQuestTierMigration.loadClientQuestsToObjectivesCompleted(clientData);
               this.storyObjectivesCompleted.addAll(objectiveStringIDs);
            } catch (Exception e) {
               System.err.println("Unknown error loading settlement client quests at level " + this.settlementData.getLevel().getIdentifier());
               e.printStackTrace();
            }
         }
      }

      this.storyObjectivesCompleted.removeIf(stringID -> StoryObjectiveRegistry.getObjectiveID(stringID) == -1);
      this.calculateCompleted();
   }

   private void calculateCompleted() {
      this.highestMajorObjectiveIndexCompleted = this.storyObjectivesCompleted
         .stream()
         .mapToInt(StoryObjectiveRegistry::getMajorObjectiveIndex)
         .max()
         .orElse(-1);
      this.highestObjectiveIndexCompleted = this.storyObjectivesCompleted.stream().mapToInt(StoryObjectiveRegistry::getSortedIndex).max().orElse(-1);
      this.totalMajorObjectivesCompleted = (int)this.storyObjectivesCompleted.stream().filter(StoryObjectiveRegistry::isMajorObjective).count();
   }

   public void fixTeamMembersCompleted() {
      boolean shouldUpdate = false;
      HashSet<Long> auths = new HashSet<>(this.calculatedAuths);

      for (ServerClient client : this.settlementData.networkData.getTeamMembers()) {
         if (this.calculatedAuths.add(client.authentication) && client.storyManager.addAllCompletedStringIDs(this.storyObjectivesCompleted)) {
            shouldUpdate = true;
         }

         auths.remove(client.authentication);
      }

      if (!auths.isEmpty()) {
         this.calculatedAuths.removeAll(auths);
      }

      if (shouldUpdate) {
         this.calculateCompleted();
      }
   }

   public void forceUpdateFromCompleted(StoryObjectiveManager manager) {
      manager.addAllCompletedStringIDs(this.storyObjectivesCompleted);
      this.calculateCompleted();
   }

   public void onTeamMemberCompletedStoryObjective(StoryObjective objective) {
      String stringID = objective.getStringID();
      if (this.storyObjectivesCompleted.add(stringID)) {
         int sortedIndex = StoryObjectiveRegistry.getSortedIndex(stringID);
         if (sortedIndex > this.highestObjectiveIndexCompleted) {
            this.highestObjectiveIndexCompleted = sortedIndex;
         }

         if (StoryObjectiveRegistry.isMajorObjective(objective.getStringID())) {
            this.totalMajorObjectivesCompleted++;
            int majorObjectiveIndex = StoryObjectiveRegistry.getMajorObjectiveIndex(stringID);
            if (majorObjectiveIndex > this.highestMajorObjectiveIndexCompleted) {
               this.highestMajorObjectiveIndexCompleted = majorObjectiveIndex;
            }

            this.settlementData.reduceRaidCounter();
         }
      }
   }

   public boolean hasCompletedStoryObjectiveOrHigher(String objectiveStringID) {
      int index = StoryObjectiveRegistry.getSortedIndex(objectiveStringID);
      if (index == -1) {
         GameLog.warn.println("Could not check hasCompletedStoryObjectiveOrHigher because \"" + objectiveStringID + "\" is not a registered objective");
         return false;
      } else {
         return this.highestObjectiveIndexCompleted >= index;
      }
   }

   public boolean hasCompletedStoryObjective(String objectiveStringID) {
      if (!StoryObjectiveRegistry.objectiveExists(objectiveStringID)) {
         GameLog.warn.println("Could not check hasCompletedStoryObjective because \"" + objectiveStringID + "\" is not a registered objective");
         return false;
      } else {
         return this.storyObjectivesCompleted.contains(objectiveStringID);
      }
   }

   public int getHighestMajorObjectiveIndexCompleted() {
      return this.highestMajorObjectiveIndexCompleted;
   }

   public int getHighestObjectiveIndexCompleted() {
      return this.highestObjectiveIndexCompleted;
   }

   public int getTotalMajorObjectivesCompleted() {
      return this.totalMajorObjectivesCompleted;
   }

   public void debugSetCurrentStoryObjective(String objectiveStringID) {
      this.storyObjectivesCompleted = new HashSet<>();
      if (objectiveStringID != null) {
         int sortedIndex = StoryObjectiveRegistry.getSortedIndex(objectiveStringID);

         for (int i = sortedIndex - 1; i >= 0; i--) {
            this.storyObjectivesCompleted.add(StoryObjectiveRegistry.getStringIDBySortedIndex(i));
         }
      } else {
         for (String sortedStringID : StoryObjectiveRegistry.getSortedStringIDs()) {
            this.storyObjectivesCompleted.add(sortedStringID);
         }
      }

      this.calculateCompleted();
   }
}
