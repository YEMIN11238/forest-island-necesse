package necesse;

import necesse.engine.GameCrashLog;
import necesse.engine.ThreadFreezeMonitor;
import necesse.engine.loading.ServerLoader;
import necesse.engine.platforms.Platform;

public class StartPlatformServer {
   public static void start(String[] args, Platform platform) {
      new ThreadFreezeMonitor(true, Thread.currentThread()).start();
      ServerLoader loader = new ServerLoader();

      try {
         if (loader.loadGame(args, platform)) {
            loader.startGame();
         }
      } catch (Exception | Error e) {
         System.err.println("Crash outside ticking, closing application.");
         e.printStackTrace();
         GameCrashLog.printCrashLog(e, null, null, "Start server crash", false);
      } finally {
         loader.unloadGame();
      }
   }
}
