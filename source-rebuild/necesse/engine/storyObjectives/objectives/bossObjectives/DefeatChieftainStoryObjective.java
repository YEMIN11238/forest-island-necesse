package necesse.engine.storyObjectives.objectives.bossObjectives;

import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class DefeatChieftainStoryObjective extends DefeatBossStoryObjective {
   public static LootTable rewards = new LootTable(new LootItem("shippingchest"));

   public DefeatChieftainStoryObjective(StoryObjectiveManager manager) {
      super(manager, "chieftain", "shippingchest");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
