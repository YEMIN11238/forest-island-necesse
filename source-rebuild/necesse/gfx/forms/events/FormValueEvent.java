package necesse.gfx.forms.events;

import necesse.gfx.forms.components.FormComponent;

public class FormValueEvent<T extends FormComponent, V> extends FormEvent<T> {
   public final V value;

   public FormValueEvent(T from, V value) {
      super(from);
      this.value = value;
   }
}
