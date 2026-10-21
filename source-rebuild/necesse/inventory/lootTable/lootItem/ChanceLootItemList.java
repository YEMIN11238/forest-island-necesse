package necesse.inventory.lootTable.lootItem;

import java.util.List;
import necesse.engine.util.GameRandom;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootTable;

public class ChanceLootItemList extends LootItemList {
   public final float chance;

   public ChanceLootItemList(float chance) {
      super();
      this.chance = chance;
   }

   public ChanceLootItemList(float chance, LootItemInterface... items) {
      super(items);
      this.chance = chance;
   }

   @Override
   public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
      LootTable.runChance(random, this.chance, lootMultiplier, remainingLootMultiplier -> super.addItems(list, random, remainingLootMultiplier, extra));
   }
}
