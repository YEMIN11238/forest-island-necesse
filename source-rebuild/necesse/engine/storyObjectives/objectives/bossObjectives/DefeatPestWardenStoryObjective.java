package necesse.engine.storyObjectives.objectives.bossObjectives;

import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class DefeatPestWardenStoryObjective extends DefeatBossStoryObjective {
   public static LootTable rewards = new LootTable(new LootItem("portalflask"));

   public DefeatPestWardenStoryObjective(StoryObjectiveManager manager) {
      super(manager, "pestwarden", "portalflask");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
