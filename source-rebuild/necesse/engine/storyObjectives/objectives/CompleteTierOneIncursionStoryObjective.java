package necesse.engine.storyObjectives.objectives;

import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class CompleteTierOneIncursionStoryObjective extends CompleteIncursionTierStoryObjective {
   public static LootTable rewards = new LootTable(new LootItem("tabletbox"), new LootItem("altardust", 100));

   public CompleteTierOneIncursionStoryObjective(StoryObjectiveManager manager) {
      super(manager, 1, "tabletbox");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
