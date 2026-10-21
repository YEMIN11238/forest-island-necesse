package necesse.gfx.forms.presets.containerComponent.object;

import necesse.engine.network.client.Client;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.lists.FormContainerInventoryList;
import necesse.inventory.container.object.OEInventoryContainer;

public class OEInventoryListContainerForm<T extends OEInventoryContainer> extends OEInventoryContainerForm<T> {
   public OEInventoryListContainerForm(Client client, T container) {
      super(client, container);
   }

   @Override
   protected void addSlots(FormFlow flow) {
      int height = 170;
      this.inventoryForm
         .addComponent(
            new FormContainerInventoryList(
               0, flow.next(height), this.inventoryForm.getWidth(), height, this.client, this.container.INVENTORY_START, this.container.INVENTORY_END
            )
         );
   }
}
