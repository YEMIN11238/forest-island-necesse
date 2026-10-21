package necesse.level.maps.levelData.jobs;

import java.util.ArrayList;
import necesse.engine.save.LoadData;
import necesse.inventory.InventoryItem;
import necesse.level.maps.LevelObject;

public abstract class MineObjectLevelJob extends TileLevelJob {
   public MineObjectLevelJob(int tileX, int tileY) {
      super(tileX, tileY);
   }

   public MineObjectLevelJob(LoadData save) {
      super(save);
   }

   public ArrayList<InventoryItem> getDroppedItems() {
      return this.getLevel().getObject(this.tileX, this.tileY).getCombinedDroppedItems(this.getLevel(), 0, this.tileX, this.tileY, "onDestroyed");
   }

   @Override
   public boolean isValid() {
      return !super.isValid() ? false : this.isValidObject(this.getObject());
   }

   public LevelObject getObject() {
      return this.getLevel().getLevelObject(this.tileX, this.tileY);
   }

   public abstract boolean isValidObject(LevelObject var1);
}
