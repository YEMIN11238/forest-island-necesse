package necesse.engine.storyObjectives.objectives.bossObjectives;

import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class DefeatCryoQueenStoryObjective extends DefeatBossStoryObjective {
   public static LootTable rewards = new LootTable(new LootItem("bannerstand"));

   public DefeatCryoQueenStoryObjective(StoryObjectiveManager manager) {
      super(manager, "cryoqueen", "bannerstand");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
