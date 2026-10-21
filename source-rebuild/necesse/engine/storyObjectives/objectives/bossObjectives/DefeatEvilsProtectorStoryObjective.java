package necesse.engine.storyObjectives.objectives.bossObjectives;

import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class DefeatEvilsProtectorStoryObjective extends DefeatBossStoryObjective {
   public static LootTable rewards = new LootTable(new LootItem("lunchbox"));

   public DefeatEvilsProtectorStoryObjective(StoryObjectiveManager manager) {
      super(manager, "evilsprotector", "lunchbox");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
