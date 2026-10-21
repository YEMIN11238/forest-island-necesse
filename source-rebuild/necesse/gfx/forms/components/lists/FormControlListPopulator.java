package necesse.gfx.forms.components.lists;

import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.gfx.forms.Form;

public interface FormControlListPopulator {
   void populateForm(Form var1, int var2, int var3, int var4, int var5, Runnable var6);

   void handleInputEvent(InputEvent var1, Form var2, Runnable var3);

   void handleControllerEvent(ControllerEvent var1, Form var2, Runnable var3);

   void runOnBindChanged(Runnable var1);
}
