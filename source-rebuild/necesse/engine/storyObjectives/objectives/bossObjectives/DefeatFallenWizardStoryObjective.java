package necesse.engine.storyObjectives.objectives.bossObjectives;

import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class DefeatFallenWizardStoryObjective extends DefeatBossStoryObjective {
   public static LootTable rewards = new LootTable(new LootItem("voidbag"));

   public DefeatFallenWizardStoryObjective(StoryObjectiveManager manager) {
      super(manager, "fallenwizard", "voidbag");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
