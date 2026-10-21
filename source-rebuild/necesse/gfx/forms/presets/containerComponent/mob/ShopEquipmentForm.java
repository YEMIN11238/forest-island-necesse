package necesse.gfx.forms.presets.containerComponent.mob;

import necesse.engine.network.client.Client;
import necesse.entity.mobs.EquipmentBuffManager;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanWorkSetting.HumanEquipmentFilterSetting;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormFlow;
import necesse.inventory.container.mob.ShopContainer;

public abstract class ShopEquipmentForm extends EquipmentForm {
   private final HumanEquipmentFilterSetting.ContainerHandler handler;

   public ShopEquipmentForm(Client client, HumanEquipmentFilterSetting.ContainerHandler handler) {
      super(
         client,
         handler.container,
         handler.EQUIPMENT_COSM_HEAD_SLOT,
         handler.EQUIPMENT_COSM_CHEST_SLOT,
         handler.EQUIPMENT_COSM_FEET_SLOT,
         handler.EQUIPMENT_HEAD_SLOT,
         handler.EQUIPMENT_CHEST_SLOT,
         handler.EQUIPMENT_FEET_SLOT,
         handler.EQUIPMENT_WEAPON_SLOT
      );
      this.handler = handler;
   }

   @Override
   public void filterEquipmentButtonPressed() {
      this.handler.setIsInFilters.runAndSend(true);
   }

   @Override
   public void onSetSelfManageEquipment(boolean value) {
      this.handler.setSelfManageEquipment.runAndSend(value);
   }

   @Override
   public void addHeader(Form form, FormFlow flow) {
      ShopContainer shopContainer = (ShopContainer)this.container;
      DialogueForm.addSettlerHeader(shopContainer.humanShop, false, shopContainer.romanceLevel, form.getWidth() - 32, form, flow);
   }

   @Override
   public HumanMob getMob() {
      return this.handler.container.humanShop;
   }

   @Override
   public EquipmentBuffManager getEquipmentManager() {
      return this.handler.container.humanShop.equipmentBuffManager;
   }
}
