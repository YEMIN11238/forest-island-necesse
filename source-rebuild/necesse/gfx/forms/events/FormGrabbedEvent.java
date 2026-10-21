package necesse.gfx.forms.events;

import necesse.engine.input.InputEvent;
import necesse.gfx.forms.components.FormComponent;

public class FormGrabbedEvent<T extends FormComponent> extends FormInputEvent<T> {
   public final boolean grabbed;

   public FormGrabbedEvent(T from, InputEvent event, boolean grabbed) {
      super(from, event);
      this.grabbed = grabbed;
   }
}
