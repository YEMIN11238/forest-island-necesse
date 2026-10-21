package necesse.engine.storyObjectives.objectives.bossObjectives;

import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class DefeatReaperStoryObjective extends DefeatBossStoryObjective {
   public static LootTable rewards = new LootTable(new LootItem("hoverboard"));

   public DefeatReaperStoryObjective(StoryObjectiveManager manager) {
      super(manager, "reaper", "hoverboard");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
