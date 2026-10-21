package necesse.engine;

import java.util.TreeMap;
import java.util.Map.Entry;

public class UpgradeCurve {
   private TreeMap<Float, Float> steps = new TreeMap<>();

   public UpgradeCurve addStep(float step, float value) {
      if (this.steps.isEmpty()) {
         this.steps.put(step, value);
      } else {
         Entry<Float, Float> lastEntry = this.steps.lastEntry();
         float lastKey = lastEntry.getKey();
         float var5 = lastEntry.getValue();
      }

      return this;
   }
}
