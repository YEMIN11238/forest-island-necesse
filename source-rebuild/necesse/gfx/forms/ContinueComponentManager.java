package necesse.gfx.forms;

import necesse.gfx.forms.components.ContinueComponent;

public interface ContinueComponentManager {
   void addContinueForm(String var1, ContinueComponent var2);

   void removeContinueForm(String var1);

   boolean hasContinueForms();
}
