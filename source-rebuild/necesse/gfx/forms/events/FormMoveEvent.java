package necesse.gfx.forms.events;

import necesse.engine.input.InputEvent;
import necesse.gfx.forms.components.FormComponent;

public class FormMoveEvent<T extends FormComponent> extends FormInputEvent<T> {
   public int x;
   public int y;

   public FormMoveEvent(T from, InputEvent event, int x, int y) {
      super(from, event);
      this.x = x;
      this.y = y;
   }
}
