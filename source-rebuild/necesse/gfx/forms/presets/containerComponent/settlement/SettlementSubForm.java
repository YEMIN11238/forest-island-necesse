package necesse.gfx.forms.presets.containerComponent.settlement;

import necesse.engine.localization.message.GameMessage;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.components.FormComponent;

public interface SettlementSubForm {
   void onSetCurrent(boolean var1);

   GameMessage getMenuButtonName();

   String getTypeString();

   default void onMenuButtonClicked(FormSwitcher switcher) {
      switcher.makeCurrent((FormComponent)this);
   }

   default SettlementToolHandler getToolHandler() {
      return null;
   }
}
