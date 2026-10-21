package necesse.gfx.forms.events;

import necesse.gfx.forms.components.FormComponent;

public class FormCursorMoveEvent<T extends FormComponent> extends FormEvent<T> {
   public final boolean causedByMouse;

   public FormCursorMoveEvent(T from, boolean causedByMouse) {
      super(from);
      this.causedByMouse = causedByMouse;
   }
}
