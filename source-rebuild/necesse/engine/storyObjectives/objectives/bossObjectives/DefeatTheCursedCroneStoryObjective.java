package necesse.engine.storyObjectives.objectives.bossObjectives;

import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class DefeatTheCursedCroneStoryObjective extends DefeatBossStoryObjective {
   public static LootTable rewards = new LootTable(new LootItem("missionboard"));

   public DefeatTheCursedCroneStoryObjective(StoryObjectiveManager manager) {
      super(manager, "thecursedcrone", "missionboard");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
