package necesse.engine.expeditions;

import java.util.List;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.matItem.MultiTextureMatItem;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.presets.CaveChestLootTable;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class CaveExpedition extends SettlerExpedition {
   public static LootTable ores = new LootTable(LootItem.between("copperore", 16, 26), LootItem.between("ironore", 12, 22), LootItem.between("goldore", 8, 18));
   public static LootTable extra = new LootTable(
      LootItem.between("batwing", 3, 8), LootItem.between("cavespidergland", 3, 6, MultiTextureMatItem.getGNDData(0))
   );
   public static LootTable rewards = new LootTable(
      LootTablePresets.basicCaveChest, new ChanceLootItemList(0.5F, LootTablePresets.caveCryptUniqueItems), ores, extra, rareHappinessObjects
   );

   @Override
   public GameMessage getUnavailableMessage() {
      return new LocalMessage("expedition", "completestory");
   }

   @Override
   public float getSuccessChance(ServerSettlementData settlement) {
      return storyProgressSuccessChance(settlement, "defeatevilsprotector");
   }

   @Override
   public int getBaseCost(ServerSettlementData settlement) {
      return 300;
   }

   @Override
   public List<InventoryItem> getRewardItems(ServerSettlementData settlement, HumanMob mob) {
      return rewards.getNewList(GameRandom.globalRandom, 1.0F);
   }

   @Override
   public List<InventoryItem> getItemIcons() {
      LootList list = new LootList();
      CaveChestLootTable.basicMainItems.addPossibleLoot(list);
      return list.getCombinedItemsAndCustomItems();
   }
}
