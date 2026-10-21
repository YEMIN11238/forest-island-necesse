package necesse.level.maps.levelData.settlementData;

import java.awt.Point;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.inventory.InventoryRange;
import necesse.inventory.item.Item;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.level.maps.Level;

public abstract class SettlementInventory extends SettlementOEInventory {
   public ItemCategoriesFilter filter = new ItemCategoriesFilter(true) {
      @Override
      public boolean isItemDisabled(Item item) {
         return super.isItemDisabled(item)
            ? true
            : SettlementInventory.this.oeInventory != null && SettlementInventory.this.oeInventory.isSettlementStorageItemDisabled(item);
      }
   };
   public int priority = 0;
   public SettlementRequestInventory fuelInventory;

   public static GameMessage getPriorityText(int priority) {
      for (SettlementInventory.Priority p : SettlementInventory.Priority.values()) {
         if (p.priorityValue == priority) {
            return p.displayName;
         }
      }

      return new StaticMessage("" + priority);
   }

   private SettlementInventory(Level level, int tileX, int tileY, boolean setup) {
      super(level, tileX, tileY, setup);
      if (setup && this.oeInventory != null) {
         this.oeInventory.setupDefaultSettlementStorage(this);
      }
   }

   public SettlementInventory(Level level, int tileX, int tileY) {
      this(level, tileX, tileY, true);
   }

   public void addSaveData(SaveData save) {
      save.addPoint("tile", new Point(this.tileX, this.tileY));
      SaveData filtersSave = new SaveData("filter");
      this.filter.addSaveData(filtersSave);
      if (!filtersSave.isEmpty()) {
         save.addSaveData(filtersSave);
      }

      save.addInt("priority", this.priority);
   }

   public static SettlementInventory fromLoadData(final ServerSettlementData data, LoadData save, int tileXOffset, int tileYOffset) throws LoadDataException {
      Point tile = save.getPoint("tile", null);
      if (tile == null) {
         throw new LoadDataException("Missing position");
      }

      tile.translate(tileXOffset, tileYOffset);
      SettlementInventory out = new SettlementInventory(data.getLevel(), tile.x, tile.y, false) {
         @Override
         public boolean isStorageValid() {
            return data.storageManager.hasInventory(this);
         }
      };
      LoadData filtersSave = save.getFirstLoadDataByName("filters");
      if (filtersSave != null) {
         out.filter.applyLoadData(filtersSave);
      }

      LoadData filterSave = save.getFirstLoadDataByName("filter");
      if (filterSave != null) {
         out.filter.applyLoadData(filterSave);
      }

      out.priority = save.getInt("priority", 0, false);
      return out;
   }

   @Override
   public InventoryRange getInventoryRange() {
      this.refreshOEInventory();
      return this.oeInventory == null ? null : this.oeInventory.getSettlementStorage();
   }

   public SettlementRequestInventory getFuelInventory() {
      this.refreshOEInventory();
      if (this.oeInventory == null) {
         return null;
      }

      SettlementRequestOptions fuelRequestOptions = this.oeInventory.getSettlementFuelRequestOptions();
      if (fuelRequestOptions != null) {
         if (this.fuelInventory == null) {
            this.fuelInventory = new SettlementRequestInventory(this.level, this.tileX, this.tileY, fuelRequestOptions) {
               @Override
               public InventoryRange getInventoryRange() {
                  this.refreshOEInventory();
                  return this.oeInventory == null ? null : this.oeInventory.getSettlementFuelInventoryRange();
               }

               @Override
               public boolean isStorageValid() {
                  return SettlementInventory.this.isStorageValid();
               }
            };
         }

         this.fuelInventory.filter = new ItemCategoriesFilter(fuelRequestOptions.minAmount, fuelRequestOptions.maxAmount, true);
         if (!this.fuelInventory.isTileValid()) {
            this.fuelInventory = null;
         }

         return this.fuelInventory;
      } else {
         this.fuelInventory = null;
         return null;
      }
   }

   @Override
   public ItemCategoriesFilter getFilter() {
      return this.filter;
   }

   public enum Priority {
      TOP(300, new LocalMessage("ui", "prioritytop")),
      HIGHER(200, new LocalMessage("ui", "priorityhigher")),
      HIGH(100, new LocalMessage("ui", "priorityhigh")),
      NORMAL(0, new LocalMessage("ui", "prioritynormal")),
      LOW(-100, new LocalMessage("ui", "prioritylow")),
      LOWER(-200, new LocalMessage("ui", "prioritylower")),
      LAST(-300, new LocalMessage("ui", "prioritylast"));

      public final int priorityValue;
      public final GameMessage displayName;

      Priority(int priorityValue, GameMessage displayName) {
         this.priorityValue = priorityValue;
         this.displayName = displayName;
      }
   }
}
