package necesse.engine.storyObjectives.objectives.bossObjectives;

import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class DefeatAscendedWizardStoryObjective extends DefeatBossStoryObjective {
   public static LootTable rewards = new LootTable(new LootItem("ringofascension"), new LootItem("upgradeshard", 300), new LootItem("alchemyshard", 300));

   public DefeatAscendedWizardStoryObjective(StoryObjectiveManager manager) {
      super(manager, "ascendedwizard", "ringofascension");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
