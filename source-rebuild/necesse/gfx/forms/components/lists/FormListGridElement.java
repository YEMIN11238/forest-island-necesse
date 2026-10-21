package necesse.gfx.forms.components.lists;

import java.awt.Rectangle;
import necesse.engine.input.InputEvent;

public abstract class FormListGridElement<E extends FormGeneralGridList> extends FormListElement<E> {
   public boolean isMouseOver(E parent) {
      if (parent.isControllerFocus(this)) {
         return true;
      }

      InputEvent event = this.getMoveEvent();
      return event == null ? false : new Rectangle(0, 0, parent.elementWidth, parent.elementHeight).contains(event.pos.hudX, event.pos.hudY);
   }
}
