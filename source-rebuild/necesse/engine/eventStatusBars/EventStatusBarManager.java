package necesse.engine.eventStatusBars;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.entity.mobs.Mob;

public class EventStatusBarManager {
   private static HashMap<Integer, EventStatusBarData> statusBars;

   public static void initialize() {
      statusBars = new HashMap<>();
   }

   public static void preGameTick(TickManager tickManager) {
      if (tickManager.isGameTick()) {
         Iterator<Entry<Integer, EventStatusBarData>> statusBarsIterator = statusBars.entrySet().iterator();

         while (statusBarsIterator.hasNext()) {
            Entry<Integer, EventStatusBarData> entry = statusBarsIterator.next();
            entry.getValue().cleanOldData();
            if (!entry.getValue().hasData()) {
               statusBarsIterator.remove();
            }
         }
      }
   }

   public static void registerMobHealthStatusBar(Mob mob) {
      registerEventStatusBar(mob.getUniqueID(), mob.getHealthUnlimited(), mob.getMaxHealth(), EventStatusBarData.BarCategory.boss, mob.getLocalization());
   }

   public static void registerEventStatusBar(int uniqueID, int current, int max, EventStatusBarData.BarCategory category, GameMessage displayName) {
      registerEventStatusBar(uniqueID, current, max, () -> new EventStatusBarData(category, displayName));
   }

   public static void registerEventStatusBar(int uniqueID, int current, int max, Supplier<EventStatusBarData> statusBarConstructor) {
      statusBars.compute(uniqueID, (key, bar) -> {
         if (bar == null) {
            bar = statusBarConstructor.get();
         }

         bar.append(current, max);
         return (EventStatusBarData)bar;
      });
   }

   public static Iterable<EventStatusBarData> getStatusBars() {
      return statusBars.values();
   }
}
