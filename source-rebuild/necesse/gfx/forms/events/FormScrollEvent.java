package necesse.gfx.forms.events;

import necesse.engine.input.InputEvent;
import necesse.gfx.forms.components.FormComponent;

public class FormScrollEvent<T extends FormComponent> extends FormInputEvent<T> {
   public int scroll;

   public FormScrollEvent(T from, InputEvent event, int scroll) {
      super(from, event);
      this.scroll = scroll;
   }
}
