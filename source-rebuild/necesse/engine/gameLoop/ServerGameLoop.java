package necesse.engine.gameLoop;

import necesse.engine.CriticalGameException;
import necesse.engine.GameCrashLog;
import necesse.engine.GameExceptionHandler;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.network.packet.PacketDisconnect;
import necesse.engine.network.server.Server;

public class ServerGameLoop extends GameLoop {
   private static GameExceptionHandler exceptionHandler = new GameExceptionHandler("server ticking");
   private final Server server;
   private long tickTimeTrack;
   private long tickTimeAverage;

   public ServerGameLoop(Server server, String name, int maxFPS) {
      super(name, maxFPS);
      this.server = server;
   }

   @Override
   public void update() {
      long tickTime = System.nanoTime();
      if (Thread.currentThread().isInterrupted()) {
         this.stopMainGameLoop();
      } else {
         try {
            if (this.server.hasClosed()) {
               Thread.currentThread().interrupt();
               return;
            }

            Performance.recordConstant(this, "tickTime", () -> {
               if (this.server != null) {
                  if (this.isGameTick()) {
                     Performance.record(this, "gameTick", this.server::tick);
                  }

                  Performance.record(this, "frameTick", () -> {
                     this.runGameLoopListenersFrameTick(null);
                     this.server.frameTick(this);
                  });
               }
            });
            exceptionHandler.clear(this.isGameTick());
         } catch (Exception e) {
            exceptionHandler.submitException(this.isGameTick(), e, () -> {
               System.err.println("Stuck in crash loop, stopping server.");
               GameCrashLog.printCrashLog(exceptionHandler.getSavedExceptions(), this.server.getLocalClient(), this.server, "Server", false);
               this.server.stop(PacketDisconnect.Code.SERVER_ERROR, s -> Thread.currentThread().interrupt());
            });
         } catch (Error e) {
            exceptionHandler.submitException(this.isGameTick(), new CriticalGameException(e), () -> {
               System.err.println("Stuck in crash loop, stopping server.");
               GameCrashLog.printCrashLog(exceptionHandler.getSavedExceptions(), this.server.getLocalClient(), this.server, "Server", false);
               this.server.stop(PacketDisconnect.Code.SERVER_ERROR, s -> Thread.currentThread().interrupt());
            });
         }

         this.tickTimeTrack = this.tickTimeTrack + (System.nanoTime() - tickTime);
      }
   }

   @Override
   public void updateSecond() {
      this.tickTimeAverage = this.tickTimeTrack / Math.max(1, this.getTPS());
      this.tickTimeTrack = 0L;
   }

   public long getTickTimeAverage() {
      return this.tickTimeAverage;
   }
}
