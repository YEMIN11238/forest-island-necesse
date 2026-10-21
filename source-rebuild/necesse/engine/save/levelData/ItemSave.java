package necesse.engine.save.levelData;

import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.VersionMigration;
import necesse.inventory.item.Item;

public class ItemSave {
   public static Item loadItem(String itemStringID) {
      if (itemStringID == null) {
         return null;
      }

      if (!ItemRegistry.itemExists(itemStringID)) {
         String newStringID = VersionMigration.tryFixStringID(itemStringID, VersionMigration.oldItemStringIDs);
         if (!itemStringID.equals(newStringID)) {
            System.out.println("Migrated item from " + itemStringID + " to " + newStringID);
            itemStringID = newStringID;
         }
      }

      return ItemRegistry.getItem(itemStringID);
   }
}
