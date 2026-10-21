package necesse.inventory.lootTable.lootItem;

import java.util.ArrayList;
import java.util.List;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.incursion.IncursionData;

public class IncursionMobTierLootItem implements LootItemInterface {
   protected LootItemInterface child;
   protected int minTier = 1;
   protected int maxReducedTier;

   public IncursionMobTierLootItem(LootItemInterface child, int maxReducedTier) {
      this.child = child;
      this.maxReducedTier = maxReducedTier;
   }

   @Override
   public void addPossibleLoot(LootList list, Object... extra) {
      LootList childList = new LootList();
      this.child.addPossibleLoot(childList, extra);

      for (Item item : childList.getItems()) {
         list.add(item);
      }

      int currentTier = this.getCurrentTier(extra);

      for (InventoryItem customItem : childList.getCustomItems()) {
         InventoryItem copy = customItem.copy();
         if (currentTier > 0) {
            copy.item.setUpgradeTier(copy, currentTier);
         }

         list.addCustom(copy);
      }
   }

   protected int getCurrentTier(Object... extra) {
      Mob mob = LootTable.expectExtra(Mob.class, extra, 0);
      if (mob != null) {
         Level level = mob.getLevel();
         if (level instanceof IncursionLevel) {
            IncursionData data = ((IncursionLevel)level).incursionData;
            if (data != null) {
               return data.getTabletTier();
            }
         }
      }

      return this.minTier;
   }

   @Override
   public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
      ArrayList<InventoryItem> childList = new ArrayList<>();
      this.child.addItems(childList, random, lootMultiplier, extra);
      int currentTier = this.getCurrentTier(extra);

      for (InventoryItem inventoryItem : childList) {
         int tier = currentTier;
         if (this.maxReducedTier > 0) {
            tier = Math.max(this.minTier, random.getIntBetween(tier - this.maxReducedTier, tier));
         }

         if (tier > 0) {
            inventoryItem.item.setUpgradeTier(inventoryItem, tier);
         }

         list.add(inventoryItem);
      }
   }
}
