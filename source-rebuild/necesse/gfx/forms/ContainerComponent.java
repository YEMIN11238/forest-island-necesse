package necesse.gfx.forms;

import necesse.engine.GlobalData;
import necesse.engine.state.MainGame;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.forms.position.FormRelativePosition;
import necesse.inventory.container.Container;

public interface ContainerComponent<T extends Container> {
   void setHidden(boolean var1);

   T getContainer();

   boolean shouldOpenInventory();

   default boolean shouldCloseInventory() {
      return false;
   }

   default boolean shouldShowInventory() {
      return this.shouldOpenInventory();
   }

   default boolean shouldShowToolbar() {
      return true;
   }

   default boolean shouldShowSidebar() {
      return true;
   }

   default boolean inventoryHotkeyClosesContainer() {
      return true;
   }

   static void setPosInventory(Form form) {
      MainGameFormManager formManager = getFormManager();
      if (formManager != null) {
         form.setPosition(
            new FormRelativePosition(
               formManager.toolbar, (formManager.toolbar.getWidth() - form.getWidth()) / 2, -form.getHeight() - form.getInterfaceStyle().formSpacing
            )
         );
      } else {
         GameWindow window = WindowManager.getWindow();
         form.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() - 75 - form.getHeight() / 2);
      }
   }

   static int getMaxInventoryHeight() {
      MainGameFormManager formManager = getFormManager();
      if (formManager != null) {
         int inventoryY = formManager.toolbar.getY() - formManager.toolbar.getInterfaceStyle().formSpacing;
         int topSpacing = 10;
         return Math.max(50, inventoryY - topSpacing);
      } else {
         return Integer.MAX_VALUE;
      }
   }

   static void setPosFocus(Form form, int xOffset) {
      MainGameFormManager formManager = getFormManager();
      if (formManager != null) {
         form.setPosition(
            new FormRelativePosition(
               formManager.inventory,
               (formManager.inventory.getWidth() - form.getWidth()) / 2 + xOffset,
               -form.getHeight() - form.getInterfaceStyle().formSpacing
            )
         );
      }
   }

   static int getMaxFocusHeight() {
      MainGameFormManager formManager = getFormManager();
      if (formManager != null) {
         int inventoryY = formManager.inventory.getY() - formManager.inventory.getInterfaceStyle().formSpacing;
         int topSpacing = 10;
         return Math.max(50, inventoryY - topSpacing);
      } else {
         return Integer.MAX_VALUE;
      }
   }

   static void setPosFocus(Form form) {
      setPosFocus(form, 0);
   }

   static MainGameFormManager getFormManager() {
      return GlobalData.getCurrentState() instanceof MainGame ? ((MainGame)GlobalData.getCurrentState()).formManager : null;
   }

   static void setPosMiddle(Form form) {
      GameWindow window = WindowManager.getWindow();
      form.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() / 2);
   }

   default void onWindowResized(GameWindow window) {
   }

   default void onContainerClosed() {
   }
}
