package necesse.inventory.item.miscItem;

import necesse.engine.GameState;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemInventory;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.world.GameClock;
import necesse.engine.world.WorldSettings;
import necesse.entity.Entity;
import necesse.entity.TileEntity;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;

public interface InternalInventoryItemInterface {
   int getInternalInventorySize();

   default Inventory getInternalInventory(InventoryItem item) {
      GNDItem gndItem = item.getGndData().getItem("inventory");
      if (gndItem instanceof GNDItemInventory) {
         GNDItemInventory gndInventory = (GNDItemInventory)gndItem;
         if (gndInventory.inventory.getSize() != this.getInternalInventorySize()) {
            gndInventory.inventory.changeSize(this.getInternalInventorySize());
         }

         return gndInventory.inventory;
      } else {
         Inventory inventory = this.getNewInternalInventory(item);
         item.getGndData().setItem("inventory", new GNDItemInventory(inventory));
         return inventory;
      }
   }

   default Inventory getNewInternalInventory(InventoryItem item) {
      return new Inventory(this.getInternalInventorySize());
   }

   default void tickInternalInventory(InventoryItem item, GameClock clock, GameState state, Entity entity, TileEntity tileEntity, WorldSettings worldSettings) {
      this.getInternalInventory(item).tickItems(clock, state, entity, tileEntity, worldSettings);
   }

   default void saveInternalInventory(InventoryItem item, Inventory inventory) {
      GNDItem gndItem = item.getGndData().getItem("inventory");
      if (gndItem instanceof GNDItemInventory) {
         GNDItemInventory gndInventory = (GNDItemInventory)gndItem;
         gndInventory.inventory.override(inventory, true, true);
      } else {
         item.getGndData().setItem("inventory", new GNDItemInventory(inventory));
      }
   }

   default boolean isValidItem(InventoryItem item) {
      return true;
   }

   static void setInternalInventory(InventoryItem item, Inventory inventory) {
      if (item.item instanceof InternalInventoryItemInterface) {
         ((InternalInventoryItemInterface)item.item).saveInternalInventory(item, inventory);
      } else {
         throw new IllegalArgumentException("InventoryItem \"" + item + "\" does not implement InternalInventoryItemInterface");
      }
   }

   default GameTooltips getPickupToggleTooltip(boolean isDisabled) {
      return isDisabled
         ? new StringTooltips(Localization.translate("itemtooltip", "autopickupdisabled"))
         : new StringTooltips(Localization.translate("itemtooltip", "autopickupenabled"));
   }

   default boolean canDisablePickup() {
      return true;
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

   default boolean canChangePouchName() {
      return true;
   }

   default String getPouchName(InventoryItem item) {
      return item.getGndData().getString("pouchName", null);
   }

   default void setPouchName(InventoryItem item, String name) {
      if (!name.isEmpty() && !name.equals(ItemRegistry.getLocalization(item.item.getID()).translate())) {
         item.getGndData().setString("pouchName", name);
      } else {
         item.getGndData().setItem("pouchName", null);
      }
   }

   default void setPouchPickupDisabled(InventoryItem item, boolean disabled) {
      item.getGndData().setBoolean("pickupDisabled", disabled);
   }

   default boolean isPickupDisabled(InventoryItem item) {
      return item.getGndData().getBoolean("pickupDisabled");
   }
}
