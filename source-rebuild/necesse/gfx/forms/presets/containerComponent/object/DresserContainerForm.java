package necesse.gfx.forms.presets.containerComponent.object;

import necesse.engine.network.client.Client;
import necesse.entity.objectEntity.DresserObjectEntity;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.containerSlot.FormContainerArmorStandSlot;
import necesse.gfx.forms.components.containerSlot.FormContainerSlot;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.armorItem.ArmorItem;

public class DresserContainerForm extends OEInventoryContainerForm<OEInventoryContainer> {
   public DresserContainerForm(Client client, OEInventoryContainer container) {
      super(client, container);
   }

   @Override
   protected void addSlots(FormFlow flow) {
      this.slots = new FormContainerSlot[this.container.INVENTORY_END - this.container.INVENTORY_START + 1];
      int currentY = flow.next();

      for (int i = 0; i < this.slots.length; i++) {
         int slotIndex = i + this.container.INVENTORY_START;
         int x = i % 10;
         if (x == 0) {
            currentY = flow.next(40);
         }

         ArmorItem.ArmorType armorType = DresserObjectEntity.getArmorType(i);
         if (armorType != null) {
            this.slots[i] = this.inventoryForm
               .addComponent(new FormContainerArmorStandSlot(this.client, this.container, slotIndex, 4 + x * 40, currentY, armorType));
         } else {
            this.slots[i] = this.inventoryForm.addComponent(new FormContainerSlot(this.client, this.container, slotIndex, 4 + x * 40, currentY));
         }
      }
   }
}
