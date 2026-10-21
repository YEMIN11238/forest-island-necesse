package necesse.engine.input.controller;

import java.util.HashMap;
import java.util.LinkedList;
import necesse.engine.util.ObjectValue;

public class SameControlBindDetector {
   private static final HashMap<Object, ControllerActionSet> usedInputActionSet = new HashMap<>();
   private static final LinkedList<ObjectValue<Object, Long>> usedStatesWithDetectionTimer = new LinkedList<>();

   public static boolean wasInputJustUsedWithDifferentAction(ControllerInputState state, Object inputUsed) {
      if (inputUsed == null) {
         return false;
      }

      ControllerActionSet actionSet = usedInputActionSet.getOrDefault(inputUsed, null);
      if (actionSet != null && actionSet != state.getActionSet()) {
         return true;
      }

      usedStatesWithDetectionTimer.add(new ObjectValue<>(inputUsed, System.currentTimeMillis() + 500L));
      usedInputActionSet.put(inputUsed, state.getActionSet());
      return false;
   }

   public static void clearUsedStatesAfterDetectionTimer() {
      while (!usedStatesWithDetectionTimer.isEmpty()) {
         ObjectValue<Object, Long> first = usedStatesWithDetectionTimer.getFirst();
         if (first.value <= System.currentTimeMillis()) {
            usedInputActionSet.remove(first.object);
            usedStatesWithDetectionTimer.removeFirst();
            continue;
         }
         break;
      }
   }
}
