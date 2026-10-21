package necesse.engine.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class WarningMessageCooldown<T> {
   public int millisecondCooldown;
   protected final HashMap<T, WarningMessageCooldown<T>.WarningTimer> warningTimers = new HashMap<>();
   protected final int timeoutMilliseconds;
   protected final LinkedList<WarningMessageCooldown<T>.WarningTimeout> timeouts;
   protected final HashSet<T> hasTimeout = new HashSet<>();

   public WarningMessageCooldown(int millisecondCooldown, int timeoutMilliseconds) {
      this.millisecondCooldown = millisecondCooldown;
      this.timeoutMilliseconds = timeoutMilliseconds;
      if (timeoutMilliseconds > 0) {
         this.timeouts = new LinkedList<>();
      } else {
         this.timeouts = null;
      }
   }

   public synchronized void tickTimeouts() {
      if (this.timeouts == null) {
         throw new IllegalStateException("Warning message cooldown are not set up to handle timeouts");
      }

      while (!this.timeouts.isEmpty()) {
         WarningMessageCooldown<T>.WarningTimeout first = this.timeouts.getFirst();
         WarningMessageCooldown<T>.WarningTimer warningTimer = this.warningTimers.get(first.key);
         if (warningTimer == null) {
            this.timeouts.removeFirst();
            this.hasTimeout.remove(first.key);
            return;
         }

         long timeSinceLastSubmit = System.currentTimeMillis() - first.timeAtLastSubmit;
         if (timeSinceLastSubmit < this.millisecondCooldown + this.timeoutMilliseconds) {
            break;
         }

         this.timeouts.removeFirst();
         if (warningTimer.timeAtLastSubmit == first.timeAtLastSubmit) {
            this.warningTimers.remove(first.key);
            this.hasTimeout.remove(first.key);
         } else {
            this.timeouts.addLast(new WarningMessageCooldown.WarningTimeout(first.key, warningTimer.timeAtLastSubmit));
         }
      }
   }

   public synchronized void submit(T key, WarningMessageCooldown.WarningHandler warningHandler) {
      WarningMessageCooldown<T>.WarningTimer warningTimer = this.warningTimers
         .compute(key, (t, last) -> last == null ? new WarningMessageCooldown.WarningTimer() : last);
      warningTimer.submit(key, warningHandler);
   }

   @FunctionalInterface
   public interface WarningHandler {
      void handleWarning(int var1);
   }

   private class WarningTimeout {
      public final T key;
      public final long timeAtLastSubmit;

      public WarningTimeout(T key, long timeAtLastSubmit) {
         this.key = key;
         this.timeAtLastSubmit = timeAtLastSubmit;
      }
   }

   private class WarningTimer {
      public int countSinceLastWarning;
      public long timeAtLastWarning;
      public long timeAtLastSubmit;

      private WarningTimer() {
      }

      public void submit(T key, WarningMessageCooldown.WarningHandler warningHandler) {
         this.timeAtLastSubmit = System.currentTimeMillis();
         long timeSinceLastWarning = System.currentTimeMillis() - this.timeAtLastWarning;
         if (timeSinceLastWarning >= WarningMessageCooldown.this.millisecondCooldown) {
            this.timeAtLastWarning = System.currentTimeMillis();
            warningHandler.handleWarning(this.countSinceLastWarning);
            this.countSinceLastWarning = 0;
         } else {
            this.countSinceLastWarning++;
         }

         if (WarningMessageCooldown.this.timeouts != null && !WarningMessageCooldown.this.hasTimeout.contains(key)) {
            WarningMessageCooldown.this.timeouts.addLast(WarningMessageCooldown.this.new WarningTimeout(key, this.timeAtLastSubmit));
            WarningMessageCooldown.this.hasTimeout.add(key);
         }
      }
   }
}
