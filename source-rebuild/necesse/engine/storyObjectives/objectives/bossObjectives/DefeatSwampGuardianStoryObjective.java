package necesse.engine.storyObjectives.objectives.bossObjectives;

import java.util.List;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.util.GameRandom;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.placeableItem.objectItem.WaystoneObjectItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class DefeatSwampGuardianStoryObjective extends DefeatBossStoryObjective {
   public static LootTable rewards = new LootTable(new LootItemInterface() {
      @Override
      public void addPossibleLoot(LootList list, Object... extra) {
         list.add("homestone");
         list.add("waystone");
      }

      @Override
      public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
         list.add(new InventoryItem("homestone"));
         ServerSettlementData data = LootTable.expectExtra(ServerSettlementData.class, extra, 1);
         if (data != null) {
            InventoryItem waystone = WaystoneObjectItem.setupWaystoneItem(new InventoryItem("waystone", 2), data.uniqueID);
            list.add(waystone);
         } else {
            list.add(new InventoryItem("coin", 500));
         }
      }
   });

   public DefeatSwampGuardianStoryObjective(StoryObjectiveManager manager) {
      super(manager, "swampguardian", "homestone");
   }

   @Override
   public LootTable getRewards() {
      return rewards;
   }
}
