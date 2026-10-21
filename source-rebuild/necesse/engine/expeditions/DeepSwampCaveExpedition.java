package necesse.engine.expeditions;

import java.util.List;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.presets.DeepCaveChestLootTable;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class DeepSwampCaveExpedition extends SettlerExpedition {
   public static LootTable ores = new LootTable(
      LootItem.between("myceliumore", 16, 26), LootItem.between("tungstenore", 12, 22), LootItem.between("lifequartz", 8, 18)
   );
   public static LootTable extra = new LootTable(LootItem.between("bone", 4, 12), LootItem.between("bonearrow", 50, 100));
   public static LootTable rewards = new LootTable(LootTablePresets.deepSwampCaveChest, ores, extra, epicHappinessObjects);

   @Override
   public GameMessage getUnavailableMessage() {
      return new LocalMessage("expedition", "completestory");
   }

   @Override
   public float getSuccessChance(ServerSettlementData settlement) {
      return storyProgressSuccessChance(settlement, "defeatpestwarden");
   }

   @Override
   public int getBaseCost(ServerSettlementData settlement) {
      return 1200;
   }

   @Override
   public List<InventoryItem> getRewardItems(ServerSettlementData settlement, HumanMob mob) {
      return rewards.getNewList(GameRandom.globalRandom, 1.0F);
   }

   @Override
   public List<InventoryItem> getItemIcons() {
      LootList list = new LootList();
      DeepCaveChestLootTable.swampMainItems.addPossibleLoot(list);
      return list.getCombinedItemsAndCustomItems();
   }
}
