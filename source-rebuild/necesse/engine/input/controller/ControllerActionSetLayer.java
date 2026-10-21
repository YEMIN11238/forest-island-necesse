package necesse.engine.input.controller;

import java.util.ArrayList;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;

public class ControllerActionSetLayer extends ControllerActionSet {
   public ControllerActionSetLayer(String name, GameMessage displayName) {
      super(name, displayName);
   }

   public void resetStates(ArrayList<ControllerEvent> outEvents, TickManager tickManager) {
      for (ControllerState state : this.states) {
         if (state instanceof ControllerButtonState) {
            ((ControllerButtonState)state).updateState(false, ControllerInput.getLatestControllerHandle(), null, outEvents, tickManager);
         } else if (state instanceof ControllerAnalogState) {
            ControllerAnalogState controllerAnalogState = (ControllerAnalogState)state;
            controllerAnalogState.updateState(0.0F, 0.0F, ControllerInput.getLatestControllerHandle(), outEvents);
         }
      }
   }
}
