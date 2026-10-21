package necesse.inventory.container.slots;

import necesse.engine.localization.Localization;
import necesse.engine.network.server.AdventureParty;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.placeableItem.consumableItem.AdventurePartyConsumableItem;

public class PartyInventoryContainerSlot extends ContainerSlot {
   public AdventureParty party;

   public PartyInventoryContainerSlot(Inventory inventory, int inventorySlot, AdventureParty party) {
      super(inventory, inventorySlot);
      this.party = party;
   }

   @Override
   public String getItemInvalidError(InventoryItem item) {
      if (item == null) {
         return null;
      } else if (this.party != null && this.party.isEmpty()) {
         return Localization.translate("ui", "adventurepartycantaddempty");
      } else {
         return item.item instanceof AdventurePartyConsumableItem
               && ((AdventurePartyConsumableItem)item.item)
                  .canAddToPartyInventory(item, this.getContainer().client, this.getInventory(), this.getInventorySlot())
            ? null
            : "";
      }
   }
}
