package necesse.entity.manager;

import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;
import necesse.inventory.InventoryItem;

public interface MobPrivateLootTableDropsListenerEntityComponent extends EntityComponent {
   void onLevelMobPrivateDropsLoot(Mob var1, ServerClient var2, Point var3, ArrayList<InventoryItem> var4);

   default int getLevelMobDropsPrivateLootPriority() {
      return 0;
   }
}
