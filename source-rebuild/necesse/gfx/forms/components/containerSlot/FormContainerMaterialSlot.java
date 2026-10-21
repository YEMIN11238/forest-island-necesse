package necesse.gfx.forms.components.containerSlot;

import java.awt.Color;
import java.awt.Rectangle;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.Container;

public class FormContainerMaterialSlot extends FormContainerSlot {
   public InventoryItem ghostItem;

   public FormContainerMaterialSlot(Client client, Container container, int containerSlotIndex, int x, int y) {
      super(client, container, containerSlotIndex, x, y);
   }

   @Override
   protected void handleActionControllerEvents(ControllerEvent event) {
      if (this.ghostItem == null || this.getContainer().getClientDraggingSlot().getItem() != null) {
         super.handleActionControllerEvents(event);
      } else if (this.isControllerFocus() && event.getState() == ControllerInput.MENU_SELECT) {
         if (event.buttonState) {
            this.ghostItem = null;
            this.playTickSound();
         }

         event.use();
      }
   }

   @Override
   protected void handleActionInputEvents(InputEvent event) {
      if (this.ghostItem == null || this.getContainer().getClientDraggingSlot().getItem() != null) {
         super.handleActionInputEvents(event);
      } else if (this.isMouseOver(event) && event.isMouseClickEvent()) {
         if (event.state) {
            this.ghostItem = null;
            this.playTickSound();
         }

         event.use();
      }
   }

   @Override
   public GameTooltips getClearTooltips() {
      return this.ghostItem != null ? null : new StringTooltips(Localization.translate("itemtooltip", "materialslot"));
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      super.draw(tickManager, perspective, renderBox);
      InventoryItem item = this.getContainerSlot().getItem();
      if (item == null) {
         if (this.ghostItem != null) {
            this.ghostItem.drawIcon(perspective, this.getX() + 4, this.getY() + 4, 32, new Color(255, 255, 255, 125));
            if (this.isHovering() && !WindowManager.getWindow().isKeyDown(-100) && !WindowManager.getWindow().isKeyDown(-99)) {
               this.addItemTooltips(this.ghostItem, perspective);
            }
         }
      } else {
         this.ghostItem = null;
      }
   }
}
