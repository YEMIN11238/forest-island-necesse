package necesse.inventory.lootTable;

import java.util.ArrayList;
import java.util.List;
import necesse.engine.util.GameRandom;
import necesse.inventory.InventoryItem;

public interface LootItemInterface {
   void addPossibleLoot(LootList var1, Object... var2);

   default void addPossibleCustomLoot(LootList list, Object... extra) {
      ArrayList<InventoryItem> items = new ArrayList<>();
      this.addItems(items, GameRandom.globalRandom, 1.0F, extra);

      for (InventoryItem item : items) {
         list.addCustom(item);
      }
   }

   void addItems(List<InventoryItem> var1, GameRandom var2, float var3, Object... var4);
}
