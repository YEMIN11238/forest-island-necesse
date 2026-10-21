package necesse.inventory.lootTable.lootItem;

import java.util.Arrays;
import java.util.List;
import necesse.engine.util.GameRandom;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootItemInterface;

public class OneOfLootItems extends LootItemList {
   public OneOfLootItems(LootItemInterface... items) {
      super();
      this.addAll(Arrays.asList(items));
   }

   @Override
   public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
      if (!this.isEmpty()) {
         random.getOneOf(this).addItems(list, random, lootMultiplier, extra);
      }
   }
}
