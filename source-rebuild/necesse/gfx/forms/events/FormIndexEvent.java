package necesse.gfx.forms.events;

import necesse.gfx.forms.components.FormComponent;

public class FormIndexEvent<T extends FormComponent> extends FormEvent<T> {
   public final int index;

   public FormIndexEvent(T from, int index) {
      super(from);
      this.index = index;
   }
}
