package necesse.gfx.forms.components;

import necesse.engine.input.InputEvent;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormEventsHandler;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.ButtonState;

public class FormTextButtonToggle extends FormTextButton implements ToggleButton<FormTextButtonToggle> {
   private FormEventsHandler<FormInputEvent<FormTextButtonToggle>> onToggle;
   protected boolean toggled;

   public FormTextButtonToggle(String text, int x, int y, int width) {
      super(text, x, y, width);
      this.handleClicksIfNoEventHandlers = true;
      this.onToggle = new FormEventsHandler<>();
      this.toggled = false;
   }

   public FormTextButtonToggle(String text, int x, int y, int width, FormInputSize size, ButtonColor color) {
      super(text, x, y, width, size, color);
      this.handleClicksIfNoEventHandlers = true;
      this.onToggle = new FormEventsHandler<>();
      this.toggled = false;
   }

   public FormTextButtonToggle onToggled(FormEventListener<FormInputEvent<FormTextButtonToggle>> listener) {
      this.onToggle.addListener(listener);
      return this;
   }

   @Override
   public ButtonState getButtonState() {
      if (!this.isActive()) {
         return ButtonState.INACTIVE;
      } else {
         return !this.isToggled() && !this.isHovering() ? ButtonState.ACTIVE : ButtonState.HIGHLIGHTED;
      }
   }

   @Override
   protected void pressed(InputEvent event) {
      this.toggled = !this.toggled;
      super.pressed(event);
      this.onToggle.onEvent(new FormInputEvent<>(this, event));
   }

   @Override
   public boolean isToggled() {
      return this.toggled;
   }

   @Override
   public void setToggled(boolean value) {
      this.toggled = value;
   }

   @Override
   public void reset() {
      this.toggled = false;
   }
}
