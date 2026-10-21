package necesse.level.maps.levelData.settlementData.storage;

import necesse.engine.util.GameLinkedList;
import necesse.inventory.InventoryItem;

public class ValidatedSettlementStorageRecord {
   public final GameLinkedList<SettlementStorageRecord>.Element element;
   public final SettlementStorageRecord record;
   public final InventoryItem invItem;

   public ValidatedSettlementStorageRecord(SettlementStorageRecord record, InventoryItem invItem) {
      this.element = null;
      this.record = record;
      this.invItem = invItem;
   }

   public ValidatedSettlementStorageRecord(GameLinkedList<SettlementStorageRecord>.Element element, InventoryItem invItem) {
      this.element = element;
      this.record = (SettlementStorageRecord)element.object;
      this.invItem = invItem;
   }
}
