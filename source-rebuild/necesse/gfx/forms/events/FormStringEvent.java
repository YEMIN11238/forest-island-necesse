package necesse.gfx.forms.events;

import necesse.gfx.forms.components.FormComponent;

public class FormStringEvent<T extends FormComponent> extends FormEvent<T> {
   public final String str;

   public FormStringEvent(T from, String str) {
      super(from);
      this.str = str;
   }

   public boolean equals(String str) {
      return this.str.equals(str);
   }

   public boolean equals(FormStringEvent event) {
      return this == event ? true : this.equals(event.str) && this.from == event.from;
   }
}
