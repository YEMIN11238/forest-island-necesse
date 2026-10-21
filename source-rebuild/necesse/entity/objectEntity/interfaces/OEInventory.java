package necesse.entity.objectEntity.interfaces;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.packet.PacketOEInventoryUpdate;
import necesse.engine.network.packet.PacketObjectEntity;
import necesse.engine.network.server.Server;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryRange;
import necesse.inventory.item.Item;
import necesse.level.maps.levelData.settlementData.SettlementInventory;
import necesse.level.maps.levelData.settlementData.SettlementRequestOptions;

public interface OEInventory {
   default void triggerInteracted() {
   }

   Inventory getInventory();

   GameMessage getInventoryName();

   default void setInventoryName(String name) {
   }

   default boolean canSetInventoryName() {
      return false;
   }

   default boolean canQuickStackInventory() {
      return true;
   }

   default boolean canRestockInventory() {
      return true;
   }

   default boolean canSortInventory() {
      return true;
   }

   default boolean canUseForNearbyCrafting() {
      return true;
   }

   default InventoryRange getSettlementStorage() {
      Inventory inventory = this.getInventory();
      return inventory != null ? new InventoryRange(inventory) : null;
   }

   default SettlementRequestOptions getSettlementFuelRequestOptions() {
      return null;
   }

   default InventoryRange getSettlementFuelInventoryRange() {
      return null;
   }

   default boolean isSettlementStorageItemDisabled(Item item) {
      return false;
   }

   default void setupDefaultSettlementStorage(SettlementInventory inventory) {
   }

   default void serverTickInventorySync(Server server, ObjectEntity ent) {
      if (server != null) {
         Inventory inventory = this.getInventory();
         if (inventory.isDirty()) {
            if (inventory.isFullDirty()) {
               server.network.sendToClientsWithEntity(new PacketObjectEntity(ent), ent);
               inventory.clean();
            } else {
               for (int i = 0; i < inventory.getSize(); i++) {
                  if (inventory.isDirty(i)) {
                     server.network.sendToClientsWithEntity(new PacketOEInventoryUpdate(this, i), ent);
                     inventory.clean(i);
                  }
               }
            }
         }
      }
   }
}
