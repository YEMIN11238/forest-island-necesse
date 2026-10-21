package necesse.engine.input.controller;

import java.util.HashSet;
import java.util.List;

public class ControllerRefreshFocusState extends ControllerState {
   private boolean submitNextEvent;

   public void updateState(HashSet<ControllerHandle> controllerHandles, List<ControllerEvent> events) {
      for (ControllerHandle controllerHandle : controllerHandles) {
         if (this.submitNextEvent) {
            events.add(ControllerEvent.customEvent(controllerHandle, this));
         }
      }

      this.submitNextEvent = false;
   }

   public void submitNextEvent() {
      this.submitNextEvent = true;
   }
}
