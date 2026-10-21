package necesse.gfx.forms.events;

import necesse.gfx.forms.components.FormComponent;

public class FormStringIndexEvent<T extends FormComponent> extends FormStringEvent<T> {
   public final int index;

   public FormStringIndexEvent(T from, String str, int index) {
      super(from, str);
      this.index = index;
   }
}
