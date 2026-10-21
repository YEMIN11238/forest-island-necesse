package necesse.entity.mobs;

import necesse.engine.network.packet.PacketMobInventory;
import necesse.engine.network.packet.PacketMobInventoryUpdate;
import necesse.engine.network.server.Server;
import necesse.inventory.Inventory;

public interface MobInventory {
   Inventory getInventory();

   default void serverTickInventorySync(Server server, Mob mob) {
      if (server != null) {
         Inventory inventory = this.getInventory();
         if (inventory.isDirty()) {
            if (inventory.isFullDirty()) {
               server.network.sendToClientsWithEntity(new PacketMobInventory(this), mob);
               inventory.clean();
            } else {
               for (int i = 0; i < inventory.getSize(); i++) {
                  if (inventory.isDirty(i)) {
                     server.network.sendToClientsWithEntity(new PacketMobInventoryUpdate(this, i), mob);
                     inventory.clean(i);
                  }
               }
            }
         }
      }
   }
}
