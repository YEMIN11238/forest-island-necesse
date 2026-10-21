package necesse.gfx.forms.presets.containerComponent.mob;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.friendly.human.humanShop.StylistHumanMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.inventory.container.mob.StylistContainer;

public class StylistContainerForm<T extends StylistContainer> extends ShopContainerForm<T> {
   public PlayerStyleForm playerStyleForm;
   public SettlerStyleForm settlerStyleForm;

   public StylistContainerForm(Client client, T container) {
      super(client, container);
      this.playerStyleForm = this.addComponent(new PlayerStyleForm(container) {
         @Override
         public void onBackPressed() {
            StylistContainerForm.this.makeCurrent(StylistContainerForm.this.dialogueForm);
         }
      });
      this.settlerStyleForm = this.addComponent(new SettlerStyleForm(container) {
         @Override
         public void onBackPressed() {
            StylistContainerForm.this.makeCurrent(StylistContainerForm.this.dialogueForm);
         }
      });
   }

   @Override
   protected void addShopDialogueOptions() {
      super.addShopDialogueOptions();
      if (this.container.humanShop instanceof StylistHumanMob
         && (
            this.container.sellingItems != null && !this.container.sellingItems.isEmpty()
               || this.container.buyingItems != null && !this.container.buyingItems.isEmpty()
         )) {
         this.dialogueForm.addDialogueOption(new LocalMessage("ui", "stylistwantchange"), () -> this.makeCurrent(this.playerStyleForm));
         if (this.container.availableSettlers != null) {
            this.dialogueForm.addDialogueOption(new LocalMessage("ui", "stylistchangesettler"), () -> this.makeCurrent(this.settlerStyleForm));
         }
      }
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      ContainerComponent.setPosFocus(this.playerStyleForm);
      ContainerComponent.setPosFocus(this.settlerStyleForm);
   }
}
