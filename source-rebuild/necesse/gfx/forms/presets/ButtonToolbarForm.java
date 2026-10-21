package necesse.gfx.forms.presets;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import necesse.engine.localization.message.GameMessage;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormButton;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormComponentList;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.ButtonIcon;

public class ButtonToolbarForm extends Form {
   private final ArrayList<ButtonToolbarForm.ToolbarButton> buttons = new ArrayList<>();

   public ButtonToolbarForm(String name) {
      super(name, 16, 40);
   }

   public ButtonToolbarForm() {
      this(null);
   }

   public void addButton(FormComponent component, BiConsumer<Integer, Integer> positionSetter, ButtonToolbarForm.ToolbarButtonShouldShow shouldShow) {
      FormComponentList list = this.addComponent(new FormComponentList());
      list.addComponent(component);
      this.buttons.add(new ButtonToolbarForm.ToolbarButton(list, component, positionSetter, shouldShow));
   }

   public void addButton(FormPositionContainer component, ButtonToolbarForm.ToolbarButtonShouldShow shouldShow) {
      this.addButton((FormComponent)component, component::setPosition, shouldShow);
   }

   public void addButton(FormContentIconButton button, ButtonToolbarForm.ToolbarButtonShouldShow shouldShow) {
      this.addButton(button, button::setPosition, shouldShow);
   }

   public FormContentIconButton addButton(
      String stringID,
      ButtonIcon icon,
      FormEventListener<FormInputEvent<FormButton>> listener,
      GameMessage tooltip,
      ButtonToolbarForm.ToolbarButtonShouldShow shouldShow
   ) {
      FormContentIconButton button = new FormContentIconButton(0, 0, FormInputSize.SIZE_32, ButtonColor.BASE, icon, tooltip);
      button.controllerFocusHashcode = stringID;
      button.onClicked(listener);
      this.addButton(button, shouldShow);
      return button;
   }

   public void addButton(FormPositionContainer component) {
      this.addButton(component, isInventoryShowing -> isInventoryShowing);
   }

   public void addButton(FormContentIconButton button) {
      this.addButton(button, isInventoryShowing -> isInventoryShowing);
   }

   public FormContentIconButton addButton(String stringID, ButtonIcon icon, FormEventListener<FormInputEvent<FormButton>> listener, GameMessage tooltip) {
      return this.addButton(stringID, icon, listener, tooltip, isInventoryShowing -> isInventoryShowing);
   }

   public void updateButtons(boolean isInventoryShowing) {
      boolean anyActive = false;
      FormFlow flow = new FormFlow(4);

      for (ButtonToolbarForm.ToolbarButton button : this.buttons) {
         boolean show = button.shouldShow.test(isInventoryShowing);
         if (show) {
            button.componentHider.setHidden(false);
            button.positionSetter.accept(flow.next(button.component.getBoundingBox().width), 4);
            anyActive = true;
         } else {
            button.componentHider.setHidden(true);
         }
      }

      this.setWidth(flow.next() + 4);
      this.setHidden(!anyActive);
   }

   private static class ToolbarButton {
      public final FormComponentList componentHider;
      public final FormComponent component;
      public final BiConsumer<Integer, Integer> positionSetter;
      public ButtonToolbarForm.ToolbarButtonShouldShow shouldShow;

      public ToolbarButton(
         FormComponentList componentHider,
         FormComponent component,
         BiConsumer<Integer, Integer> positionSetter,
         ButtonToolbarForm.ToolbarButtonShouldShow shouldShow
      ) {
         this.componentHider = componentHider;
         this.component = component;
         this.positionSetter = positionSetter;
         this.shouldShow = shouldShow;
      }
   }

   public interface ToolbarButtonShouldShow {
      boolean test(boolean var1);
   }
}
