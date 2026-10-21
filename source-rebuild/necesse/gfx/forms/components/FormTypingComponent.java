package necesse.gfx.forms.components;

import necesse.engine.GameLog;
import necesse.engine.input.Control;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.localization.message.GameMessage;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.forms.FormManager;
import necesse.gfx.forms.events.FormEvent;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormEventsHandler;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.inventory.InventoryItem;
import org.lwjgl.system.Platform;

public abstract class FormTypingComponent extends FormComponent {
   private static FormTypingComponent currentTypingComponent;
   protected FormEventsHandler<FormInputEvent<FormTypingComponent>> inputEvents = new FormEventsHandler<>();
   protected FormEventsHandler<FormEvent<FormTypingComponent>> changeEvents = new FormEventsHandler<>();
   protected GameMessage controllerTypingHeader = null;

   public static boolean isCurrentlyTyping() {
      return currentTypingComponent != null && !currentTypingComponent.isDisposed();
   }

   public static boolean appendItemToTyping(InventoryItem item) {
      if (item == null) {
         return false;
      } else {
         return isCurrentlyTyping() ? currentTypingComponent.appendItem(item) : false;
      }
   }

   public static boolean appendTextToTyping(String text) {
      if (text == null) {
         return false;
      } else {
         return isCurrentlyTyping() ? currentTypingComponent.appendText(text) : false;
      }
   }

   public static boolean submitBackspaceToTyping() {
      return isCurrentlyTyping() ? currentTypingComponent.submitBackspace() : false;
   }

   public static FormTypingComponent getCurrentTypingComponent() {
      return currentTypingComponent;
   }

   private static boolean isCurrentlyTyping(FormTypingComponent component) {
      return currentTypingComponent == component;
   }

   private static void stopCurrentlyTyping(FormTypingComponent component) {
      if (component == null) {
         throw new NullPointerException("Stop typing component null");
      }

      if (currentTypingComponent == component) {
         component.changedTyping(false);
         currentTypingComponent = null;
      }
   }

   private static void setCurrentlyTyping(FormTypingComponent component) {
      if (component == null) {
         throw new NullPointerException("Typing component null");
      }

      if (currentTypingComponent != component) {
         if (currentTypingComponent != null) {
            currentTypingComponent.changedTyping(false);
         }

         component.changedTyping(true);
         if (Input.lastInputIsController) {
            component.playTickSound();
            component.setCaretEnd();
            component.getManager().openControllerKeyboard(component);
         }
      }

      Control.resetControls();
      currentTypingComponent = component;
   }

   public FormTypingComponent onInputEvent(FormEventListener<FormInputEvent<FormTypingComponent>> listener) {
      this.inputEvents.addListener(listener);
      return this;
   }

   public FormTypingComponent onChange(FormEventListener<FormEvent<FormTypingComponent>> listener) {
      this.changeEvents.addListener(listener);
      return this;
   }

   public void changedTyping(boolean value) {
   }

   public final void setTyping(boolean value) {
      if (value) {
         FormManager manager = this.getManager();
         if (manager == null) {
            GameLog.warn.println("Cannot set typing on component not yet added to form manager");
         } else {
            setCurrentlyTyping(this);
            manager.setNextControllerFocus(this);
         }
      } else {
         stopCurrentlyTyping(this);
      }
   }

   public void setCaretEnd() {
   }

   public boolean appendItem(InventoryItem item) {
      return false;
   }

   public boolean isValidAppendText(String text) {
      return true;
   }

   public boolean appendText(String text) {
      return false;
   }

   public boolean submitBackspace() {
      return false;
   }

   public boolean submitControllerEnter(InputEvent event) {
      return false;
   }

   public FairTypeDrawOptions getDrawOptions() {
      return null;
   }

   public FairTypeDrawOptions getTextBoxDrawOptions() {
      return null;
   }

   public FormTypingComponent setControllerTypingHeader(GameMessage message) {
      this.controllerTypingHeader = message;
      return this;
   }

   public GameMessage getControllerTypingHeader() {
      return this.controllerTypingHeader;
   }

   public boolean submitTypingEvent(InputEvent event, boolean allowNavigation) {
      return false;
   }

   public final boolean isTyping() {
      return isCurrentlyTyping(this);
   }

   public boolean isControllerTyping() {
      FormManager manager = this.getManager();
      return manager != null ? manager.isControllerTyping(this) : false;
   }

   protected void submitChangeEvent() {
      this.changeEvents.onEvent(new FormEvent<>(this));
   }

   public abstract void submitUsedInputEvent(InputEvent var1);

   @Override
   public void dispose() {
      if (this.isTyping()) {
         stopCurrentlyTyping(this);
      }

      super.dispose();
   }

   public static int getSystemPasteKey() {
      switch (Platform.get()) {
         case WINDOWS:
            return 341;
         case MACOSX:
            return 341;
         default:
            return 341;
      }
   }

   public static int getSystemShiftWordKey() {
      switch (Platform.get()) {
         case WINDOWS:
            return 341;
         case MACOSX:
            return 342;
         default:
            return 341;
      }
   }
}
