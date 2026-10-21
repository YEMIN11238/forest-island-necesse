package necesse.engine.storyObjectives.objectives.bossObjectives;

import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class DefeatAncientVultureStoryObjective extends DefeatBossStoryObjective {
   public static LootTable rewards = new LootTable(new LootItem("recallflask"));

   public DefeatAncientVultureStoryObjective(StoryObjectiveManager manager) {
      super(manager, "ancientvulture", "recallflask");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
