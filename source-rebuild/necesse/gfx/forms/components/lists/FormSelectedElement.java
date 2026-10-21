package necesse.gfx.forms.components.lists;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.gameTooltips.GameTooltipManager;

public abstract class FormSelectedElement<E extends FormSelectedList> extends FormListElement<E> {
   private boolean selected;

   public final boolean isSelected() {
      return this.selected;
   }

   public final void clearSelected() {
      this.selected = false;
   }

   final void makeSelected(FormSelectedList parent) {
      if (parent != null) {
         this.selected = true;
      }
   }

   protected boolean onlyAcceptLeftClick() {
      return true;
   }

   protected void onClick(E parent, int elementIndex, InputEvent event, PlayerMob perspective) {
      if (!this.onlyAcceptLeftClick() || event.getID() == -100) {
         parent.setSelected(elementIndex);
      }
   }

   protected void onControllerEvent(E parent, int elementIndex, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      if (!this.onlyAcceptLeftClick() || event.getState() == ControllerInput.MENU_SELECT) {
         parent.setSelected(elementIndex);
         event.use();
      }
   }

   @Override
   public void drawControllerFocus(ControllerFocus current) {
      super.drawControllerFocus(current);
      GameTooltipManager.addControllerGlyph(Localization.translate("ui", "selectbutton"), ControllerInput.MENU_SELECT);
   }
}
