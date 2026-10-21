package necesse.gfx.forms.events;

import necesse.gfx.forms.components.FormCheckBox;

public class FormCheckboxesEventHandler {
   public final FormCheckBox[] checkBoxes;

   public FormCheckboxesEventHandler(FormCheckBox[] checkBoxes) {
      this.checkBoxes = checkBoxes;
   }

   public FormCheckboxesEventHandler onClicked(FormEventListener<FormCheckboxesClickEvent> listener) {
      for (int i = 0; i < this.checkBoxes.length; i++) {
         int finalIndex = i;
         this.checkBoxes[i].onClicked(e -> {
            FormCheckboxesClickEvent newEvent = new FormCheckboxesClickEvent(e.from, e.event, finalIndex);
            listener.onEvent(newEvent);
            if (newEvent.hasPreventedDefault()) {
               e.preventDefault();
            }
         });
      }

      return this;
   }

   public boolean[] getStates() {
      boolean[] states = new boolean[this.checkBoxes.length];

      for (int i = 0; i < this.checkBoxes.length; i++) {
         states[i] = this.checkBoxes[i].checked;
      }

      return states;
   }
}
