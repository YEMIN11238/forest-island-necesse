package necesse.level.maps.levelData.settlementData;

import necesse.engine.localization.message.GameMessage;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.level.maps.Level;

public abstract class SettlementOEInventory extends LevelStorage {
   public OEInventory oeInventory;

   public SettlementOEInventory(Level level, int tileX, int tileY, boolean setup) {
      super(level, tileX, tileY);
      if (setup) {
         this.refreshOEInventory();
      }
   }

   public void refreshOEInventory() {
      ObjectEntity objectEntity = this.level.entityManager.getObjectEntity(this.tileX, this.tileY);
      if (objectEntity instanceof OEInventory) {
         this.oeInventory = (OEInventory)objectEntity;
      } else {
         this.oeInventory = null;
      }
   }

   @Override
   public GameMessage getInventoryName() {
      this.refreshOEInventory();
      return this.oeInventory == null ? this.level.getObjectName(this.tileX, this.tileY) : this.oeInventory.getInventoryName();
   }
}
