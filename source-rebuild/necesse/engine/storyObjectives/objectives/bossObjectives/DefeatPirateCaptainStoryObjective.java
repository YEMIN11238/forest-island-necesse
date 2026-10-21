package necesse.engine.storyObjectives.objectives.bossObjectives;

import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class DefeatPirateCaptainStoryObjective extends DefeatBossStoryObjective {
   public static LootTable rewards = new LootTable(new LootItem("coinpouch"));

   public DefeatPirateCaptainStoryObjective(StoryObjectiveManager manager) {
      super(manager, "piratecaptain", "coinpouch");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
