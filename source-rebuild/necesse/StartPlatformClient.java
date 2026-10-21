package necesse;

import necesse.engine.GameCrashLog;
import necesse.engine.ThreadFreezeMonitor;
import necesse.engine.loading.ClientLoader;
import necesse.engine.platforms.Platform;

public class StartPlatformClient {
   public static void start(String[] args, Platform platform) {
      new ThreadFreezeMonitor(false, Thread.currentThread()).start();
      ClientLoader loader = new ClientLoader();

      try {
         if (loader.loadGame(args, platform)) {
            loader.startGame();
         }
      } catch (Exception | Error e) {
         System.err.println("Crash outside ticking, closing application.");
         e.printStackTrace();
         GameCrashLog.printCrashLog(e, null, null, "Start client crash", true);
      } finally {
         loader.unloadGame();
      }
   }
}
