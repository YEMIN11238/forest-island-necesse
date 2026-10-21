package necesse.engine.storyObjectives.objectives;

import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class CompleteTierFiveIncursionStoryObjective extends CompleteIncursionTierStoryObjective {
   public static LootTable rewards = new LootTable(new LootItem("wormholelocket"), new LootItem("upgradeshard", 125), new LootItem("alchemyshard", 125));

   public CompleteTierFiveIncursionStoryObjective(StoryObjectiveManager manager) {
      super(manager, 5, "wormholelocket");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
