package necesse.gfx.forms.presets.containerComponent.item;

import java.awt.Rectangle;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.client.Client;
import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.presets.containerComponent.ContainerForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.item.RenameItemContainer;

public class RenameItemContainerForm<T extends RenameItemContainer> extends ContainerForm<T> {
   public FormTextInput inputField;
   public FormLocalTextButton renameButton;

   public RenameItemContainerForm(Client client, T container) {
      super(client, 300, 300, container);
      FormFlow flow = new FormFlow(10);
      if (!container.itemSlot.isClear()) {
         this.addComponent(
            (FormLocalLabel)flow.nextY(
               new FormLocalLabel(
                  ItemRegistry.getLocalization(container.itemSlot.getItem().item.getID()),
                  new FontOptions(20),
                  0,
                  this.getWidth() / 2,
                  10,
                  this.getWidth() - 20
               ),
               10
            )
         );
      }

      this.inputField = this.addComponent(flow.nextY(new FormTextInput(4, 0, FormInputSize.SIZE_24, this.getWidth() - 8, RenameItemContainer.MAX_NAME_LENGTH)));
      this.inputField.rightClickToClear = true;
      this.inputField.onSubmit(e -> container.renameButton.runAndSend(this.inputField.getText()));
      if (!container.itemSlot.isClear()) {
         InventoryItem item = container.itemSlot.getItem();
         GNDItem currentName = item.getGndData().getItem("name");
         if (currentName != null && !GNDItem.isDefault(currentName)) {
            this.inputField.setText(currentName.toString());
         }
      }

      flow.next(10);
      int renameButtonWidth = Math.min(150, this.getWidth() - 20);
      this.renameButton = this.addComponent(
         flow.nextY(
            new FormLocalTextButton(
               "ui", "renamebutton", this.getWidth() / 2 - renameButtonWidth / 2, 60, renameButtonWidth, FormInputSize.SIZE_24, ButtonColor.BASE
            ),
            10
         )
      );
      this.renameButton.onClicked(e -> container.renameButton.runAndSend(this.inputField.getText()));
      this.setHeight(flow.next());
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      this.renameButton.setActive(this.container.canRename(this.inputField.getText()));
      super.draw(tickManager, perspective, renderBox);
   }
}
