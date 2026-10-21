package necesse.level.maps.levelData.settlementData.settler;

import java.util.HashMap;
import java.util.Map.Entry;
import necesse.engine.GameLog;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;

public class SettlerRecreationExhaustionManager {
   public static String SOCIALIZING_IDENTIFIER = "socializing";
   public static String RELAXING_IDENTIFIER = "relaxing";
   public static String EXERCISING_IDENTIFIER = "exercise";
   public static String BEAUTY_IDENTIFIER = "beauty";
   public static String ENTERTAINMENT_IDENTIFIER = "entertainment";
   public final SettlerMob settlerMob;
   private final HashMap<String, Float> recreationExhaustion = new HashMap<>();

   public SettlerRecreationExhaustionManager(SettlerMob settlerMob) {
      this.settlerMob = settlerMob;
   }

   public void addSaveData(SaveData save) {
      SaveData exhaustionSave = new SaveData("recreationExhaustion");

      for (Entry<String, Float> entry : this.recreationExhaustion.entrySet()) {
         float value = entry.getValue();
         if (value > 0.0F) {
            exhaustionSave.addFloat(entry.getKey(), value);
         }
      }

      if (!exhaustionSave.isEmpty()) {
         save.addSaveData(exhaustionSave);
      }
   }

   public void applyLoadData(LoadData save) {
      this.recreationExhaustion.clear();
      LoadData exhaustionSave = save.getFirstLoadDataByName("recreationExhaustion");
      if (exhaustionSave != null) {
         for (LoadData data : exhaustionSave.getLoadData()) {
            try {
               float value = Float.parseFloat(data.getData());
               this.recreationExhaustion.put(data.getName(), GameMath.limit(value, 0.0F, 1.0F));
            } catch (NumberFormatException e) {
               GameLog.warn.println("Could not load recreation exhaustion with identifier " + data.getName() + " and value " + data.getData());
            }
         }
      }
   }

   public float getRecreationRegenModifier(String identifier) {
      float value = Math.min(this.recreationExhaustion.getOrDefault(identifier, 0.0F), 1.0F);
      return 1.0F - value;
   }

   public void regenRecreation(float delta, String identifier) {
      this.recreationExhaustion.compute(identifier, (key, preValue) -> {
         if (preValue == null) {
            preValue = 0.0F;
         }

         return Math.min(preValue + delta * 0.5F, 1.0F);
      });
   }

   public void useRecreation(float delta) {
      delta /= Math.max(this.settlerMob.getSettlerQualityOfLife() / 2.0F, 1.0F);

      for (Entry<String, Float> entry : this.recreationExhaustion.entrySet()) {
         float lastValue = entry.getValue();
         float newValue = Math.max(0.0F, lastValue - delta);
         entry.setValue(newValue);
      }
   }

   public String getDebugTooltip() {
      StringBuilder stringBuilder = new StringBuilder("[");
      boolean first = true;

      for (Entry<String, Float> entry : this.recreationExhaustion.entrySet()) {
         float value = entry.getValue();
         if (!(value <= 0.0F)) {
            if (!first) {
               stringBuilder.append(", ");
            }

            first = false;
            stringBuilder.append(entry.getKey()).append("=").append(Math.round(value * 100.0F)).append("%");
         }
      }

      return stringBuilder.append("]").toString();
   }
}
