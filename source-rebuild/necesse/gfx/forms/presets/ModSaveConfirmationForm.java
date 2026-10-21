package necesse.gfx.forms.presets;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.window.WindowManager;

public class ModSaveConfirmationForm extends ConfirmationForm {
   public ModSaveConfirmationForm(String name) {
      super(name);
   }

   public void setupModSaveConfirmation(Runnable restartLaterAction) {
      Runnable restart = ModsForm.restartGameRunnable();
      this.setupConfirmation(
         new LocalMessage("ui", "modssavenotice"),
         new LocalMessage("ui", restart != null ? "modsrestart" : "modsquit"),
         new LocalMessage("ui", "modslater"),
         () -> {
            if (restart != null) {
               restart.run();
            } else {
               WindowManager.getWindow().requestClose();
            }
         },
         restartLaterAction
      );
   }
}
