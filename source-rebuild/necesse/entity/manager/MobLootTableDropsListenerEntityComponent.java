package necesse.entity.manager;

import java.awt.Point;
import java.util.ArrayList;
import necesse.entity.mobs.Mob;
import necesse.inventory.InventoryItem;

public interface MobLootTableDropsListenerEntityComponent extends EntityComponent {
   void onLevelMobDropsLoot(Mob var1, Point var2, ArrayList<InventoryItem> var3);

   default int getLevelMobDropsLootPriority() {
      return 0;
   }
}
